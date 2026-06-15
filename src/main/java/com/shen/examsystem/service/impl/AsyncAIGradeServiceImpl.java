package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shen.examsystem.dto.AIGradeResponse;
import com.shen.examsystem.entity.AnswerDetail;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.mapper.AnswerDetailMapper;
import com.shen.examsystem.mapper.ExamRecordMapper;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.service.AIService;
import com.shen.examsystem.service.AsyncAIGradeService;
import com.shen.examsystem.service.SubjectiveGradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 异步AI判分服务实现类
 */
@Service
@Slf4j
public class AsyncAIGradeServiceImpl implements AsyncAIGradeService {

    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private AnswerDetailMapper answerDetailMapper;
    
    @Autowired
    private QuestionBankMapper questionBankMapper;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private SubjectiveGradingService subjectiveGradingService;

    @Override
    @Async("aiGradeExecutor")
    public void executeAIGrade(Long recordId) {
        log.info("开始异步AI判分，记录ID: {}", recordId);
        
        // 等待主事务提交，确保数据可见
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            log.error("考试记录不存在: {}", recordId);
            return;
        }
        
        // 更新判分状态为"判分中"
        record.setAiGradeStatus(1);
        examRecordMapper.updateById(record);
        
        try {
            // 获取所有答题明细
            LambdaQueryWrapper<AnswerDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AnswerDetail::getRecordId, recordId);
            List<AnswerDetail> details = answerDetailMapper.selectList(wrapper);
            
            BigDecimal subjectiveScore = BigDecimal.ZERO;
            BigDecimal totalScore = record.getObjectiveScore() != null ? record.getObjectiveScore() : BigDecimal.ZERO;
            
            for (AnswerDetail detail : details) {
                QuestionBank question = questionBankMapper.selectById(detail.getQuestionId());
                if (question == null || question.getType() <= 3) {
                    // 跳过客观题
                    if (question != null && detail.getScore() != null) {
                        totalScore = totalScore.add(detail.getScore());
                    }
                    continue;
                }
                
                // 主观题AI判分
                String studentAnswer = detail.getStudentAnswer();
                if (studentAnswer == null || studentAnswer.isEmpty()) {
                    continue;
                }
                
                try {
                    // 构造题目内容
                    String questionContent = question.getContent();
                    if (questionContent.startsWith("{")) {
                        try {
                            Map<String, Object> contentMap = new com.google.gson.Gson().fromJson(
                                questionContent, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                            if (contentMap.containsKey("title")) {
                                questionContent = contentMap.get("title").toString();
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    // 调用AI判分
                    AIGradeResponse aiResponse = aiService.gradeSubjective(
                        questionContent,
                        question.getStandardAnswer() != null ? question.getStandardAnswer() : "",
                        studentAnswer,
                        question.getScore().doubleValue()
                    );
                    
                    BigDecimal score = BigDecimal.valueOf(aiResponse.getScore());
                    
                    // 构造详细判分依据
                    StringBuilder reasonBuilder = new StringBuilder();
                    reasonBuilder.append("【AI判分结果】\n");
                    reasonBuilder.append(String.format("得分：%.1f分（满分%.1f分）\n", score.doubleValue(), question.getScore().doubleValue()));
                    reasonBuilder.append(String.format("判分依据：%s\n", aiResponse.getReason()));
                    
                    if (aiResponse.getKeywords() != null && !aiResponse.getKeywords().isEmpty()) {
                        reasonBuilder.append("答对的关键点：").append(String.join("、", aiResponse.getKeywords())).append("\n");
                    }
                    if (aiResponse.getMissing() != null && !aiResponse.getMissing().isEmpty()) {
                        reasonBuilder.append("遗漏的关键点：").append(String.join("、", aiResponse.getMissing()));
                    }
                    
                    // 更新答题明细
                    detail.setScore(score);
                    detail.setAiReason(reasonBuilder.toString());
                    answerDetailMapper.updateById(detail);
                    
                    subjectiveScore = subjectiveScore.add(score);
                    totalScore = totalScore.add(score);
                    
                    log.info("AI判分完成 - 记录ID: {}, 题目ID: {}, 得分: {}", recordId, detail.getQuestionId(), score);
                    
                } catch (Exception e) {
                    log.error("AI判分失败，使用传统NLP - 记录ID: {}, 题目ID: {}", recordId, detail.getQuestionId(), e);
                    
                    // 降级到传统NLP判分
                    BigDecimal score = BigDecimal.valueOf(subjectiveGradingService.gradeSubjective(
                        studentAnswer,
                        question.getStandardAnswer() != null ? question.getStandardAnswer() : "",
                        question.getScore().doubleValue()
                    ));
                    
                    detail.setScore(score);
                    detail.setAiReason(String.format("AI判分失败，使用余弦相似度计算得分：%.1f分（满分%.1f分）", 
                        score.doubleValue(), question.getScore().doubleValue()));
                    answerDetailMapper.updateById(detail);
                    
                    subjectiveScore = subjectiveScore.add(score);
                    totalScore = totalScore.add(score);
                }
            }
            
            // 更新考试记录
            record.setSubjectiveScore(subjectiveScore);
            record.setTotalScore(totalScore);
            record.setAiGradeStatus(2); // 判分完成
            record.setStatus(1); // 已批阅
            examRecordMapper.updateById(record);
            
            log.info("异步AI判分完成 - 记录ID: {}, 总分: {}", recordId, totalScore);
            
        } catch (Exception e) {
            log.error("异步AI判分异常 - 记录ID: {}", recordId, e);
            
            // 更新判分状态为失败
            record.setAiGradeStatus(3);
            examRecordMapper.updateById(record);
        }
    }
}
