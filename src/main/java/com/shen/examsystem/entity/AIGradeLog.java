package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI判分日志实体类
 */
@Data
@TableName("ai_grade_log")
public class AIGradeLog {
    
    /**
     * 日志ID (雪花算法)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 考试记录ID
     */
    private Long recordId;
    
    /**
     * 题目ID
     */
    private Long questionId;
    
    /**
     * 学生作答内容
     */
    private String studentAnswer;
    
    /**
     * 标准答案
     */
    private String standardAnswer;
    
    /**
     * AI评分
     */
    private Double aiScore;
    
    /**
     * AI判分依据
     */
    private String aiReason;
    
    /**
     * 答对的关键点(JSON)
     */
    private String keywords;
    
    /**
     * 遗漏的关键点(JSON)
     */
    private String missing;
    
    /**
     * 使用的AI模型
     */
    private String model;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
