package com.shen.examsystem.dto;

import lombok.Data;

/**
 * AI判分请求DTO
 */
@Data
public class AIGradeRequest {
    
    /**
     * 题目内容
     */
    private String question;
    
    /**
     * 标准答案
     */
    private String standardAnswer;
    
    /**
     * 学生答案
     */
    private String studentAnswer;
    
    /**
     * 满分
     */
    private Double fullScore;
}
