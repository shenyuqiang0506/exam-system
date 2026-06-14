package com.shen.examsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组卷规则 DTO
 * 用于遗传算法智能组卷的输入参数
 */
public class PaperRuleDTO {

    private String title;
    private String subjectName;
    private BigDecimal targetDifficulty;
    private BigDecimal totalScore;
    private Integer singleChoiceCount;
    private BigDecimal singleChoiceScore;
    private Integer multiChoiceCount;
    private BigDecimal multiChoiceScore;
    private Integer trueFalseCount;
    private BigDecimal trueFalseScore;
    private Integer subjectiveCount;
    private BigDecimal subjectiveScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private List<Long> classIds;
    private Integer enableMonitor;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public BigDecimal getTargetDifficulty() { return targetDifficulty; }
    public void setTargetDifficulty(BigDecimal targetDifficulty) { this.targetDifficulty = targetDifficulty; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public Integer getSingleChoiceCount() { return singleChoiceCount; }
    public void setSingleChoiceCount(Integer singleChoiceCount) { this.singleChoiceCount = singleChoiceCount; }

    public BigDecimal getSingleChoiceScore() { return singleChoiceScore; }
    public void setSingleChoiceScore(BigDecimal singleChoiceScore) { this.singleChoiceScore = singleChoiceScore; }

    public Integer getMultiChoiceCount() { return multiChoiceCount; }
    public void setMultiChoiceCount(Integer multiChoiceCount) { this.multiChoiceCount = multiChoiceCount; }

    public BigDecimal getMultiChoiceScore() { return multiChoiceScore; }
    public void setMultiChoiceScore(BigDecimal multiChoiceScore) { this.multiChoiceScore = multiChoiceScore; }

    public Integer getTrueFalseCount() { return trueFalseCount; }
    public void setTrueFalseCount(Integer trueFalseCount) { this.trueFalseCount = trueFalseCount; }

    public BigDecimal getTrueFalseScore() { return trueFalseScore; }
    public void setTrueFalseScore(BigDecimal trueFalseScore) { this.trueFalseScore = trueFalseScore; }

    public Integer getSubjectiveCount() { return subjectiveCount; }
    public void setSubjectiveCount(Integer subjectiveCount) { this.subjectiveCount = subjectiveCount; }

    public BigDecimal getSubjectiveScore() { return subjectiveScore; }
    public void setSubjectiveScore(BigDecimal subjectiveScore) { this.subjectiveScore = subjectiveScore; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public List<Long> getClassIds() { return classIds; }
    public void setClassIds(List<Long> classIds) { this.classIds = classIds; }

    public Integer getEnableMonitor() { return enableMonitor; }
    public void setEnableMonitor(Integer enableMonitor) { this.enableMonitor = enableMonitor; }
}
