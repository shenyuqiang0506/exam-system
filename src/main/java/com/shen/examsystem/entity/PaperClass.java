package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷班级关联实体类
 * 对应表: paper_class
 */
@Data
@TableName("paper_class")
public class PaperClass {

    /** 关联ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 班级ID */
    private Long classId;

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
