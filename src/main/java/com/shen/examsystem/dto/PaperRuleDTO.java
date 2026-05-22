package com.shen.examsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组卷规则 DTO
 * 用于遗传算法智能组卷的输入参数
 */
@Data
public class PaperRuleDTO {

    /** 试卷名称 */
    private String title;

    /** 科目名称 */
    private String subjectName;

    /** 期望难度系数 (0.1 ~ 1.0) */
    private BigDecimal targetDifficulty;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 单选题数量 (题型=1) */
    private Integer singleChoiceCount;
    /** 单选题单分 */
    private BigDecimal singleChoiceScore;

    /** 多选题数量 (题型=2) */
    private Integer multiChoiceCount;
    /** 多选题单分 */
    private BigDecimal multiChoiceScore;

    /** 主观题数量 (题型=4) */
    private Integer subjectiveCount;
    /** 主观题单分 */
    private BigDecimal subjectiveScore;

    /** 考试开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 考试结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 分配的班级ID列表 */
    private List<Long> classIds;
}
