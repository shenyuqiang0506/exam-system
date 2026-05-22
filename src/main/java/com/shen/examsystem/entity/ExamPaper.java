package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷实体类
 * 对应表: exam_paper
 */
@Data
@TableName("exam_paper")
public class ExamPaper {

    /** 试卷ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 试卷名称 */
    private String title;

    /** 科目名称 */
    private String subjectName;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 期望平均难度系数 */
    private BigDecimal targetDifficulty;

    /** 考试开始时间 */
    private LocalDateTime startTime;

    /** 考试结束时间 */
    private LocalDateTime endTime;

    /** 状态: 0-正常, 1-已归档 */
    private Integer isArchived;

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
