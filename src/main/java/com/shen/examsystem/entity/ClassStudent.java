package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级学生关联实体类
 * 对应表: class_student
 */
@Data
@TableName("class_student")
public class ClassStudent {

    /** 关联ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 学生ID */
    private Long studentId;

    /** 加入时间 */
    private LocalDateTime joinTime;

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
