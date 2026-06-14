package com.shen.examsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shen.examsystem.dto.AIGradeResponse;
import com.shen.examsystem.dto.AIGenerateQuestionResponse;
import com.shen.examsystem.dto.AISaveQuestionRequest;
import com.shen.examsystem.entity.AIGradeLog;
import com.shen.examsystem.entity.QuestionBank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI服务接口 - DeepSeek
 */
public interface AIService {
    
    /**
     * AI主观题判分
     * 
     * @param question       题目内容
     * @param standardAnswer 标准答案
     * @param studentAnswer  学生答案
     * @param fullScore      满分
     * @return AI判分结果（分数+判分依据）
     */
    AIGradeResponse gradeSubjective(String question, String standardAnswer, 
                                     String studentAnswer, double fullScore);
    
    /**
     * AI智能出题
     * 
     * @param subject    科目名称
     * @param knowledge  知识点
     * @param type       题型（1单选/2多选/3判断/4主观）
     * @param difficulty 难度（1-5）
     * @param count      数量
     * @return 生成的题目列表
     */
    List<AIGenerateQuestionResponse> generateQuestions(String subject, String knowledge,
                                                        Integer type, Integer difficulty, 
                                                        Integer count);
    
    /**
     * 保存AI生成的题目到题库
     * 
     * @param request 题目信息
     * @param teacherId 教师ID
     * @return 保存后的题目
     */
    QuestionBank saveQuestion(AISaveQuestionRequest request, Long teacherId);
    
    /**
     * 批量保存AI生成的题目到题库
     * 
     * @param questions 题目列表
     * @param subjectName 科目名称
     * @param type 题型
     * @param teacherId 教师ID
     * @return 保存的题目数量
     */
    int saveQuestions(List<AIGenerateQuestionResponse> questions, String subjectName, 
                      Integer type, Long teacherId);
    
    /**
     * 查询AI判分日志列表（分页）
     * 
     * @param page      页码
     * @param size      每页数量
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    IPage<AIGradeLog> getGradeLogs(Integer page, Integer size, 
                                    LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询AI判分日志详情
     * 
     * @param id 日志ID
     * @return 日志详情
     */
    AIGradeLog getGradeLogById(Long id);
}
