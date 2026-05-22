package com.shen.examsystem.service;

import com.shen.examsystem.dto.PaperRuleDTO;
import com.shen.examsystem.entity.QuestionBank;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 遗传算法智能组卷服务
 */
@Service
public class GeneticAlgorithmService {

    /** 种群大小 */
    private static final int POPULATION_SIZE = 20;
    /** 最大迭代次数 */
    private static final int MAX_GENERATIONS = 100;
    /** 交叉概率 */
    private static final double CROSSOVER_RATE = 0.8;
    /** 变异概率 */
    private static final double MUTATION_RATE = 0.1;
    /** 提前终止适应度阈值 */
    private static final double FITNESS_THRESHOLD = 0.98;

    private final Random random = new Random();

    /**
     * 遗传算法生成试卷
     *
     * @param rule          组卷规则
     * @param allQuestionsDb 该科目所有候选题目 (从数据库一次性查出)
     * @return 最优试卷的题目列表
     */
    public List<QuestionBank> generatePaper(PaperRuleDTO rule, List<QuestionBank> allQuestionsDb) {
        // ============ 1. 预处理与熔断 ============
        // 按题型分组
        Map<Integer, List<QuestionBank>> questionsByType = allQuestionsDb.stream()
                .collect(Collectors.groupingBy(QuestionBank::getType));

        // 构建题型-数量需求映射
        Map<Integer, Integer> typeCountMap = buildTypeCountMap(rule);
        // 构建题型-分值映射
        Map<Integer, BigDecimal> typeScoreMap = buildTypeScoreMap(rule);

        // 校验库存是否充足
        for (Map.Entry<Integer, Integer> entry : typeCountMap.entrySet()) {
            int type = entry.getKey();
            int required = entry.getValue();
            List<QuestionBank> available = questionsByType.getOrDefault(type, Collections.emptyList());
            if (available.size() < required) {
                throw new RuntimeException("题库库存不足：题型 " + type + " 需要 " + required + " 道，实际仅有 " + available.size() + " 道");
            }
        }

        double targetDifficulty = rule.getTargetDifficulty().doubleValue();

        // ============ 2. 初始化种群 ============
        List<List<QuestionBank>> population = initializePopulation(typeCountMap, questionsByType);

        // ============ 3~7. 迭代进化 ============
        List<QuestionBank> bestPaper = null;
        double bestFitness = -1;

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // 计算每个个体的适应度
            double[] fitnessScores = new double[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                fitnessScores[i] = calculateFitness(population.get(i), targetDifficulty, typeScoreMap, rule.getTotalScore());
                if (fitnessScores[i] > bestFitness) {
                    bestFitness = fitnessScores[i];
                    bestPaper = new ArrayList<>(population.get(i));
                }
            }

            // 终止条件：适应度达到阈值
            if (bestFitness >= FITNESS_THRESHOLD) {
                break;
            }

            // 选择：轮盘赌生成交配池
            List<List<QuestionBank>> matingPool = rouletteWheelSelection(population, fitnessScores);

            // 交叉与变异生成新一代
            List<List<QuestionBank>> newPopulation = new ArrayList<>();
            while (newPopulation.size() < POPULATION_SIZE) {
                // 随机选择两个父代
                int p1Idx = random.nextInt(matingPool.size());
                int p2Idx = random.nextInt(matingPool.size());
                List<QuestionBank> parent1 = matingPool.get(p1Idx);
                List<QuestionBank> parent2 = matingPool.get(p2Idx);

                // 交叉
                List<QuestionBank> child = crossover(parent1, parent2, typeCountMap, questionsByType);

                // 变异
                child = mutate(child, questionsByType);

                newPopulation.add(child);
            }
            population = newPopulation;
        }

        // ============ 8. 返回结果 ============
        return bestPaper != null ? bestPaper : population.get(0);
    }

    // ==================== 内部方法 ====================

    /**
     * 构建题型-数量需求映射
     */
    private Map<Integer, Integer> buildTypeCountMap(PaperRuleDTO rule) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, rule.getSingleChoiceCount());
        map.put(2, rule.getMultiChoiceCount());
        map.put(3, rule.getTrueFalseCount() != null ? rule.getTrueFalseCount() : 0);
        map.put(4, rule.getSubjectiveCount());
        return map;
    }

    /**
     * 构建题型-分值映射
     */
    private Map<Integer, BigDecimal> buildTypeScoreMap(PaperRuleDTO rule) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        map.put(1, rule.getSingleChoiceScore());
        map.put(2, rule.getMultiChoiceScore());
        map.put(3, rule.getTrueFalseScore() != null ? rule.getTrueFalseScore() : BigDecimal.ZERO);
        map.put(4, rule.getSubjectiveScore());
        return map;
    }

    /**
     * 2. 初始化种群
     */
    private List<List<QuestionBank>> initializePopulation(
            Map<Integer, Integer> typeCountMap,
            Map<Integer, List<QuestionBank>> questionsByType) {

        List<List<QuestionBank>> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(generateRandomPaper(typeCountMap, questionsByType));
        }
        return population;
    }

    /**
     * 随机生成一张试卷（个体/染色体）
     */
    private List<QuestionBank> generateRandomPaper(
            Map<Integer, Integer> typeCountMap,
            Map<Integer, List<QuestionBank>> questionsByType) {

        List<QuestionBank> paper = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : typeCountMap.entrySet()) {
            int type = entry.getKey();
            int count = entry.getValue();
            List<QuestionBank> pool = questionsByType.get(type);
            // 使用 Set 去重，确保单张试卷内无重复题
            Set<Integer> selectedIndices = new HashSet<>();
            while (selectedIndices.size() < count) {
                int idx = random.nextInt(pool.size());
                selectedIndices.add(idx);
            }
            for (int idx : selectedIndices) {
                paper.add(pool.get(idx));
            }
        }
        return paper;
    }

    /**
     * 3. 适应度函数
     * f = 1 - |actualDifficulty - targetDifficulty|
     */
    private double calculateFitness(
            List<QuestionBank> paper,
            double targetDifficulty,
            Map<Integer, BigDecimal> typeScoreMap,
            BigDecimal totalScore) {

        // 计算加权平均难度
        double weightedDifficultySum = 0;
        for (QuestionBank q : paper) {
            BigDecimal score = typeScoreMap.get(q.getType());
            weightedDifficultySum += q.getDifficulty().doubleValue() * score.doubleValue();
        }
        double actualDifficulty = weightedDifficultySum / totalScore.doubleValue();

        // 适应度
        return 1 - Math.abs(actualDifficulty - targetDifficulty);
    }

    /**
     * 4. 选择算子：轮盘赌算法
     */
    private List<List<QuestionBank>> rouletteWheelSelection(
            List<List<QuestionBank>> population, double[] fitnessScores) {

        // 计算总适应度（如果有负值则平移）
        double minFitness = Arrays.stream(fitnessScores).min().orElse(0);
        double offset = minFitness < 0 ? -minFitness + 0.01 : 0;

        double totalFitness = 0;
        double[] adjustedFitness = new double[fitnessScores.length];
        for (int i = 0; i < fitnessScores.length; i++) {
            adjustedFitness[i] = fitnessScores[i] + offset;
            totalFitness += adjustedFitness[i];
        }

        // 构建交配池
        List<List<QuestionBank>> matingPool = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double spin = random.nextDouble() * totalFitness;
            double cumulative = 0;
            for (int j = 0; j < adjustedFitness.length; j++) {
                cumulative += adjustedFitness[j];
                if (cumulative >= spin) {
                    matingPool.add(new ArrayList<>(population.get(j)));
                    break;
                }
            }
        }
        return matingPool;
    }

    /**
     * 5. 交叉算子：单点交叉
     * 随机选中一种题型，将两张试卷中该题型的部分题目互换
     */
    private List<QuestionBank> crossover(
            List<QuestionBank> parent1,
            List<QuestionBank> parent2,
            Map<Integer, Integer> typeCountMap,
            Map<Integer, List<QuestionBank>> questionsByType) {

        // 按概率决定是否交叉
        if (random.nextDouble() > CROSSOVER_RATE) {
            return new ArrayList<>(parent1);
        }

        // 按题型分组父代题目
        Map<Integer, List<QuestionBank>> p1ByType = groupByType(parent1);
        Map<Integer, List<QuestionBank>> p2ByType = groupByType(parent2);

        // 随机选择一种题型进行交叉
        List<Integer> types = new ArrayList<>(typeCountMap.keySet());
        int crossType = types.get(random.nextInt(types.size()));

        // 构建子代：交叉题型用 parent2 的，其余用 parent1 的
        List<QuestionBank> child = new ArrayList<>();
        for (Integer type : types) {
            if (type == crossType) {
                child.addAll(p2ByType.getOrDefault(type, Collections.emptyList()));
            } else {
                child.addAll(p1ByType.getOrDefault(type, Collections.emptyList()));
            }
        }

        // 校验是否有重复题，若有则放弃交叉返回 parent1
        Set<Long> ids = new HashSet<>();
        for (QuestionBank q : child) {
            if (!ids.add(q.getId())) {
                return new ArrayList<>(parent1);
            }
        }

        return child;
    }

    /**
     * 6. 变异算子
     * 随机选中一道题，从同题型题库中随机挑一道其他题目替换
     */
    private List<QuestionBank> mutate(
            List<QuestionBank> paper,
            Map<Integer, List<QuestionBank>> questionsByType) {

        if (random.nextDouble() > MUTATION_RATE) {
            return paper;
        }

        // 随机选中一道题的索引
        int mutateIdx = random.nextInt(paper.size());
        QuestionBank original = paper.get(mutateIdx);
        int type = original.getType();

        // 从同题型题库中随机选一道不同的题目
        List<QuestionBank> pool = questionsByType.get(type);
        Set<Long> existingIds = paper.stream().map(QuestionBank::getId).collect(Collectors.toSet());

        List<QuestionBank> candidates = pool.stream()
                .filter(q -> !existingIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!candidates.isEmpty()) {
            QuestionBank replacement = candidates.get(random.nextInt(candidates.size()));
            paper.set(mutateIdx, replacement);
        }

        return paper;
    }

    /**
     * 将题目列表按题型分组
     */
    private Map<Integer, List<QuestionBank>> groupByType(List<QuestionBank> questions) {
        return questions.stream().collect(Collectors.groupingBy(QuestionBank::getType));
    }
}
