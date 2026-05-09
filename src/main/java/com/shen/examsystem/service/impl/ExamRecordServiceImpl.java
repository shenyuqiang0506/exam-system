package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.shen.examsystem.service.ExamRecordService;
import com.shen.examsystem.service.SubjectiveGradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 考试记录 Service 实现类
 */
@Service
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
        // 1. 获取试卷中的所有题目（用于判分）
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, paperId);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        // 构建题目ID -> 题目信息的映射
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        for (PaperQuestion pq : paperQuestions) {
            QuestionBank question = questionBankMapper.selectById(pq.getQuestionId());
            if (question != null) {
                questionMap.put(question.getId(), question);
            }
        }

        // 2. 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setPaperId(paperId);
        record.setStatus(1); // 已交卷/已批阅
        record.setTotalScore(BigDecimal.ZERO);
        record.setObjectiveScore(BigDecimal.ZERO);
        record.setSubjectiveScore(BigDecimal.ZERO);
        this.save(record);

        // 3. 保存答题明细并计算分数
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;
        BigDecimal subjectiveScore = BigDecimal.ZERO;

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
            String aiReason = null;

            if (question != null) {
                int questionType = question.getType();
                String standardAnswer = question.getStandardAnswer();
                BigDecimal questionScore = question.getScore();

                if (questionType <= 3) {
                    // 客观题：精确匹配
                    if (standardAnswer != null && standardAnswer.equalsIgnoreCase(answerText)) {
                        score = questionScore;
                    }
                    objectiveScore = objectiveScore.add(score);
                } else {
                    // 主观题：使用 NLP 判分
                    if (answerText != null && !answerText.isEmpty()) {
                        score = BigDecimal.valueOf(subjectiveGradingService.gradeSubjective(
                            answerText,
                            standardAnswer != null ? standardAnswer : "",
                            questionScore.doubleValue()
                        ));
                        aiReason = String.format("基于余弦相似度计算得分：%.1f分（满分%.1f分）",
                            score.doubleValue(), questionScore.doubleValue());
                    }
                    subjectiveScore = subjectiveScore.add(score);
                }
            }

            AnswerDetail detail = new AnswerDetail();
            detail.setRecordId(record.getId());
            detail.setQuestionId(questionId);
            detail.setStudentAnswer(answerText);
            detail.setScore(score);
            detail.setAiReason(aiReason);
            answerDetailMapper.insert(detail);

            totalScore = totalScore.add(score);
        }

        // 4. 更新记录分数
        record.setTotalScore(totalScore);
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(subjectiveScore);
        this.updateById(record);

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
            map.put("createTime", record.getCreateTime() != null ? record.getCreateTime().format(formatter) : "");
            result.add(map);
        }
        return result;
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
}
