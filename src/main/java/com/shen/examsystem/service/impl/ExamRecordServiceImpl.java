package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.dto.AIGradeResponse;
import com.shen.examsystem.entity.AnswerDetail;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.mapper.AnswerDetailMapper;
import com.shen.examsystem.mapper.ExamPaperMapper;
import com.shen.examsystem.mapper.ExamRecordMapper;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.service.AIService;
import com.shen.examsystem.service.AsyncAIGradeService;
import com.shen.examsystem.service.ExamRecordService;
import com.shen.examsystem.service.SubjectiveGradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试记录 Service 实现类
 */
@Service
@Slf4j
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {

    @Autowired
    private AnswerDetailMapper answerDetailMapper;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private SubjectiveGradingService subjectiveGradingService;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private AsyncAIGradeService asyncAIGradeService;

    @Override
    public List<ExamRecord> listByPaperId(Long paperId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getPaperId, paperId)
               .eq(ExamRecord::getStatus, 1);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitPaper(Long studentId, Long paperId, List<?> answers) {
        // 1. 并发保护：再次检查是否已交卷
        LambdaQueryWrapper<ExamRecord> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ExamRecord::getStudentId, studentId)
                    .eq(ExamRecord::getPaperId, paperId)
                    .eq(ExamRecord::getStatus, 1);
        if (this.count(existWrapper) > 0) {
            throw new RuntimeException("您已完成该试卷考试，不可重复交卷");
        }

        // 2. 获取试卷中的所有题目（用于判分）
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, paperId);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        // 构建题目ID -> 题目信息的映射
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        boolean hasSubjective = false;
        for (PaperQuestion pq : paperQuestions) {
            QuestionBank question = questionBankMapper.selectById(pq.getQuestionId());
            if (question != null) {
                questionMap.put(question.getId(), question);
                if (question.getType() == 4) {
                    hasSubjective = true;
                }
            }
        }

        // 3. 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setPaperId(paperId);
        
        // 判断是否有主观题，决定是否需要异步AI判分
        if (hasSubjective) {
            record.setStatus(2); // 待AI批阅
            record.setAiGradeStatus(1); // 判分中
        } else {
            record.setStatus(1); // 已交卷/已批阅
            record.setAiGradeStatus(0); // 无需AI判分
        }
        
        record.setTotalScore(BigDecimal.ZERO);
        record.setObjectiveScore(BigDecimal.ZERO);
        record.setSubjectiveScore(BigDecimal.ZERO);
        this.save(record);

        // 4. 保存答题明细并计算客观题分数
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;

        for (Object item : answers) {
            Long questionId;
            String answerText;

            if (item instanceof Map) {
                Map<?, ?> answer = (Map<?, ?>) item;
                questionId = Long.valueOf(answer.get("questionId").toString());
                answerText = answer.get("answer") != null ? answer.get("answer").toString() : "";
            } else {
                try {
                    java.lang.reflect.Method getQId = item.getClass().getMethod("getQuestionId");
                    java.lang.reflect.Method getAns = item.getClass().getMethod("getAnswer");
                    questionId = (Long) getQId.invoke(item);
                    Object ansObj = getAns.invoke(item);
                    answerText = ansObj != null ? ansObj.toString() : "";
                } catch (Exception e) {
                    throw new RuntimeException("答案数据格式错误", e);
                }
            }

            QuestionBank question = questionMap.get(questionId);
            BigDecimal score = BigDecimal.ZERO;

            if (question != null) {
                int questionType = question.getType();
                String standardAnswer = question.getStandardAnswer();
                BigDecimal questionScore = question.getScore();

                if (questionType <= 3) {
                    // 客观题：精确匹配（多选题排序后比较）
                    if (standardAnswer != null) {
                        String sortedStandard = sortAnswer(standardAnswer);
                        String sortedStudent = sortAnswer(answerText);
                        if (sortedStandard.equalsIgnoreCase(sortedStudent)) {
                            score = questionScore;
                        }
                    }
                    objectiveScore = objectiveScore.add(score);
                    totalScore = totalScore.add(score);
                }
                // 主观题先不计分，等异步AI判分完成后再计算
            }

            AnswerDetail detail = new AnswerDetail();
            detail.setRecordId(record.getId());
            detail.setQuestionId(questionId);
            detail.setStudentAnswer(answerText);
            detail.setScore(question != null && question.getType() <= 3 ? score : null); // 主观题分数暂时为空
            detail.setAiReason(null); // 主观题AI判分依据暂时为空
            answerDetailMapper.insert(detail);
        }

        // 5. 更新记录客观题分数
        record.setObjectiveScore(objectiveScore);
        record.setTotalScore(totalScore);
        this.updateById(record);

        // 6. 如果有主观题，触发异步AI判分
        if (hasSubjective) {
            log.info("触发异步AI判分 - 记录ID: {}", record.getId());
            asyncAIGradeService.executeAIGrade(record.getId());
        }

        return record.getId();
    }

    @Override
    public List<Map<String, Object>> getMyRecords(Long studentId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
               .orderByDesc(ExamRecord::getCreateTime);
        List<ExamRecord> records = this.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ExamRecord record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("paperId", record.getPaperId());
            // 查询真实试卷标题
            ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
            map.put("paperTitle", paper != null ? paper.getTitle() : "试卷 #" + record.getPaperId());
            map.put("totalScore", record.getTotalScore());
            map.put("objectiveScore", record.getObjectiveScore());
            map.put("subjectiveScore", record.getSubjectiveScore());
            map.put("status", record.getStatus());
            map.put("aiGradeStatus", record.getAiGradeStatus());
            map.put("createTime", record.getCreateTime() != null ? record.getCreateTime().format(formatter) : "");
            
            // AI判分状态描述
            String aiGradeStatusDesc = getAIGradeStatusDesc(record.getAiGradeStatus());
            map.put("aiGradeStatusDesc", aiGradeStatusDesc);
            
            result.add(map);
        }
        return result;
    }
    
    /**
     * 获取AI判分状态描述
     */
    private String getAIGradeStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "";
            case 1: return "AI判分中...";
            case 2: return "AI判分完成";
            case 3: return "AI判分失败";
            default: return "";
        }
    }

    @Override
    public boolean hasCompletedExam(Long studentId, Long paperId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
               .eq(ExamRecord::getPaperId, paperId)
               .eq(ExamRecord::getStatus, 1); // 已交卷
        return this.count(wrapper) > 0;
    }

    @Override
    public ExamRecord getOngoingExam(Long studentId, Long paperId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
               .eq(ExamRecord::getPaperId, paperId)
               .eq(ExamRecord::getStatus, 0) // 考试中
               .orderByDesc(ExamRecord::getCreateTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public ExamRecord getStudentRecord(Long studentId, Long paperId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
               .eq(ExamRecord::getPaperId, paperId)
               .orderByDesc(ExamRecord::getCreateTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public Map<String, Object> getRecordDetail(Long recordId) {
        ExamRecord record = this.getById(recordId);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }

        // 查询答题明细
        LambdaQueryWrapper<AnswerDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerDetail::getRecordId, recordId);
        List<AnswerDetail> details = answerDetailMapper.selectList(wrapper);

        // 构建包含题目信息的明细列表
        List<Map<String, Object>> detailList = new ArrayList<>();
        for (AnswerDetail detail : details) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("id", detail.getId());
            detailMap.put("questionId", detail.getQuestionId());
            detailMap.put("studentAnswer", detail.getStudentAnswer());
            detailMap.put("score", detail.getScore());
            detailMap.put("aiReason", detail.getAiReason());

            // 获取题目信息
            QuestionBank question = questionBankMapper.selectById(detail.getQuestionId());
            if (question != null) {
                detailMap.put("questionContent", question.getContent());
                detailMap.put("questionType", question.getType());
                detailMap.put("standardAnswer", question.getStandardAnswer());
                detailMap.put("fullScore", question.getScore());
            }
            detailList.add(detailMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("details", detailList);
        return result;
    }

    /**
     * 对答案字符串排序（用于多选题比较）
     * 例如 "ACB" -> "ABC", "C,A,B" -> "A,B,C"
     */
    private String sortAnswer(String answer) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }
        // 移除空格和逗号
        String cleaned = answer.replaceAll("[,\\s]", "");
        char[] chars = cleaned.toUpperCase().toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
