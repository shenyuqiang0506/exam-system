package com.shen.examsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 组卷规则 DTO
 * 用于遗传算法智能组卷的输入参数
 */
@Data
public class PaperRuleDTO {

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
}
