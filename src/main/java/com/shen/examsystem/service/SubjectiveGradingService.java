package com.shen.examsystem.service;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 主观题自动判分服务
 * 基于 HanLP 分词 + 空间向量模型 (VSM) + 余弦相似度
 */
@Service
public class SubjectiveGradingService {

    /**
     * 主观题判分核心方法
     *
     * @param studentAnswer  学生作答内容
     * @param standardAnswer 标准答案 (可包含给分关键词)
     * @param fullScore      该题满分
     * @return 最终得分 (四舍五入保留一位小数)
     */
    public double gradeSubjective(String studentAnswer, String standardAnswer, double fullScore) {
        // ============ 6. 防御性编程 ============
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return 0.0;
        }
        if (standardAnswer == null || standardAnswer.isBlank()) {
            throw new RuntimeException("标准答案不能为空，无法进行判分");
        }

        // ============ 2. 文本预处理与分词 ============
        List<String> studentWords = segmentToWordList(studentAnswer);
        List<String> standardWords = segmentToWordList(standardAnswer);

        // ============ 3. 构建词频向量空间 ============
        Map<String, Integer> studentVector = buildWordFrequencyVector(studentWords);
        Map<String, Integer> standardVector = buildWordFrequencyVector(standardWords);

        // 合并两个向量的 key，确保维度一致
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(studentVector.keySet());
        vocabulary.addAll(standardVector.keySet());

        // 补齐缺失维度 (词频为 0)
        for (String word : vocabulary) {
            studentVector.putIfAbsent(word, 0);
            standardVector.putIfAbsent(word, 0);
        }

        // ============ 4. 计算余弦相似度 ============
        double similarity = calculateCosineSimilarity(studentVector, standardVector);

        // ============ 5. 计算得分 ============
        double score = similarity * fullScore;
        // 四舍五入保留一位小数
        BigDecimal bd = BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 2. 分词：使用 HanLP 标准分词器，提取词语列表
     */
    private List<String> segmentToWordList(String text) {
        List<Term> termList = StandardTokenizer.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word.trim();
            // 过滤掉单个标点符号和空白
            if (!word.isEmpty() && word.length() > 1 || isChineseOrLetter(word)) {
                words.add(word);
            }
        }
        return words;
    }

    /**
     * 判断是否为中文字符或英文字母 (保留有意义的单字词)
     */
    private boolean isChineseOrLetter(String word) {
        if (word.length() != 1) return true;
        char c = word.charAt(0);
        return Character.isLetter(c);
    }

    /**
     * 3. 构建词频向量
     */
    private Map<String, Integer> buildWordFrequencyVector(List<String> words) {
        Map<String, Integer> vector = new HashMap<>();
        for (String word : words) {
            vector.merge(word, 1, Integer::sum);
        }
        return vector;
    }

    /**
     * 4. 计算余弦相似度
     * cosine(A, B) = (A · B) / (|A| * |B|)
     */
    private double calculateCosineSimilarity(Map<String, Integer> vectorA, Map<String, Integer> vectorB) {
        // 分子：内积 (点乘)
        double dotProduct = 0;
        for (String key : vectorA.keySet()) {
            dotProduct += vectorA.get(key) * vectorB.getOrDefault(key, 0);
        }

        // 分母：两个向量的模长之积
        double normA = 0;
        for (int val : vectorA.values()) {
            normA += (double) val * val;
        }
        normA = Math.sqrt(normA);

        double normB = 0;
        for (int val : vectorB.values()) {
            normB += (double) val * val;
        }
        normB = Math.sqrt(normB);

        // 处理分母为 0 的极端情况
        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    // ============ 测试 main 方法 ============
    public static void main(String[] args) {
        SubjectiveGradingService service = new SubjectiveGradingService();

        String standardAnswer = "Spring Boot是一个基于Spring框架的快速开发脚手架，核心思想是约定优于配置。";
        double fullScore = 10.0;

        String studentA = "Spring Boot是基于Spring的，它主要理念就是约定优于配置，能快速开发。";
        String studentB = "不知道，这题太难了。";
        String studentC = "";  // 空答案
        String studentD = "Spring Boot是一个基于Spring框架的快速开发脚手架，核心思想是约定优于配置。"; // 完全正确

        System.out.println("========== 主观题判分测试 ==========");
        System.out.println("标准答案: " + standardAnswer);
        System.out.println("满分: " + fullScore);
        System.out.println("-----------------------------------");

        double scoreA = service.gradeSubjective(studentA, standardAnswer, fullScore);
        System.out.println("学生A答案: " + studentA);
        System.out.println("学生A得分: " + scoreA);
        System.out.println();

        double scoreB = service.gradeSubjective(studentB, standardAnswer, fullScore);
        System.out.println("学生B答案: " + studentB);
        System.out.println("学生B得分: " + scoreB);
        System.out.println();

        double scoreC = service.gradeSubjective(studentC, standardAnswer, fullScore);
        System.out.println("学生C答案: (空)");
        System.out.println("学生C得分: " + scoreC);
        System.out.println();

        double scoreD = service.gradeSubjective(studentD, standardAnswer, fullScore);
        System.out.println("学生D答案: " + studentD);
        System.out.println("学生D得分: " + scoreD);
    }
}
