package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答题明细实体类
 * 对应表: answer_detail
 * 记录学生每道题的作答详情和得分
 */
@Data
@TableName("answer_detail")
public class AnswerDetail {

    /** 明细ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 考试记录ID */
    private Long recordId;

    /** 题目ID */
    private Long questionId;

    /** 学生作答内容 */
    private String studentAnswer;

    /** 该题最终得分 */
    private BigDecimal score;

    /** 主观题算法判分依据/评语 */
    private String aiReason;

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
