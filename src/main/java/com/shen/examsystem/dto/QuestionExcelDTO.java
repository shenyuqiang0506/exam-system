package com.shen.examsystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 题目 Excel 导入 DTO
 * 对应 Excel 模板列
 */
@Data
public class QuestionExcelDTO {

    @ExcelProperty("科目")
    private String subjectName;

    @ExcelProperty("题型")
    private Integer type;

    @ExcelProperty("题目正文")
    private String content;

    @ExcelProperty("标准答案")
    private String standardAnswer;

    @ExcelProperty("给分点")
    private String pointsKeyword;

    @ExcelProperty("默认分值")
    private BigDecimal score;

    @ExcelProperty("难度系数")
    private BigDecimal difficulty;
}
