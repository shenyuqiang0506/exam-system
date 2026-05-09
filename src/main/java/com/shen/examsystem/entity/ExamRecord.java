package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试记录实体类
 * 对应表: exam_record
 * 记录学生每次参加考试的整体情况
 */
@Data
@TableName("exam_record")
public class ExamRecord {

    /** 记录ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学生用户ID */
    private Long studentId;

    /** 试卷ID */
    private Long paperId;

    /** 状态: 0-考试中, 1-已交卷/已批阅 */
    private Integer status;

    /** 最终总得分 */
    private BigDecimal totalScore;

    /** 客观题得分 */
    private BigDecimal objectiveScore;

    /** 主观题得分 */
    private BigDecimal subjectiveScore;

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
