package com.shen.examsystem.service;

/**
 * 异步AI判分服务接口
 */
public interface AsyncAIGradeService {
    
    /**
     * 异步执行AI判分
     * 
     * @param recordId 考试记录ID
     */
    void executeAIGrade(Long recordId);
}
