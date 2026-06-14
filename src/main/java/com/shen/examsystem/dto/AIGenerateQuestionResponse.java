package com.shen.examsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * AI出题响应DTO
 */
@Data
public class AIGenerateQuestionResponse {
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 选项列表 (单选/多选题4个选项，判断题2个选项，主观题为空)
     */
    private List<String> options;
    
    /**
     * 正确答案
     */
    private String answer;
    
    /**
     * 解析
     */
    private String analysis;
    
    /**
     * 题型 (1单选/2多选/3判断/4主观)
     */
    private Integer type;
}
