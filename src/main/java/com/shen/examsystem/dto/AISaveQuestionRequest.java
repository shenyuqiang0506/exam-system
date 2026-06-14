package com.shen.examsystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI题目保存请求DTO
 */
@Data
public class AISaveQuestionRequest {
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 题型 (1单选/2多选/3判断/4主观)
     */
    private Integer type;
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 选项列表 (单选/多选题)
     */
    private List<String> options;
    
    /**
     * 标准答案
     */
    private String standardAnswer;
    
    /**
     * 分值
     */
    private BigDecimal score;
    
    /**
     * 难度系数 (0.1~1.0)
     */
    private BigDecimal difficulty;
    
    /**
     * 解析
     */
    private String analysis;
}
