package com.shen.examsystem.dto;

import lombok.Data;

/**
 * AI出题请求DTO
 */
@Data
public class AIGenerateQuestionRequest {
    
    /**
     * 科目名称
     */
    private String subject;
    
    /**
     * 知识点
     */
    private String knowledge;
    
    /**
     * 题型 (1单选/2多选/3判断/4主观)
     */
    private Integer type;
    
    /**
     * 难度 (1-5)
     */
    private Integer difficulty;
    
    /**
     * 生成数量
     */
    private Integer count;
}
