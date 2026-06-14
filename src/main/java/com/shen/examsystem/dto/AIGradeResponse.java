package com.shen.examsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * AI判分响应DTO
 */
@Data
public class AIGradeResponse {
    
    /**
     * 得分
     */
    private Double score;
    
    /**
     * 判分依据
     */
    private String reason;
    
    /**
     * 学生答对的关键点
     */
    private List<String> keywords;
    
    /**
     * 学生遗漏的关键点
     */
    private List<String> missing;
}
