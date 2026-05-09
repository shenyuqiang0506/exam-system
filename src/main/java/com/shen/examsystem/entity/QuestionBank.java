package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题库实体类
 * 对应表: question_bank
 */
@Data
@TableName("question_bank")
public class QuestionBank {

    /** 题目ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 科目名称 */
    private String subjectName;

    /** 题型: 1-单选, 2-多选, 3-判断, 4-主观题 */
    private Integer type;

    /** 题目正文 (含选项JSON) */
    private String content;

    /** 标准答案 */
    private String standardAnswer;

    /** 给分关键词 (逗号分隔, 仅主观题) */
    private String pointsKeyword;

    /** 题目默认分值 */
    private BigDecimal score;

    /** 难度系数 (0.1~1.0) */
    private BigDecimal difficulty;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
