package com.shen.examsystem.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计分析 Mapper
 * 全部使用 @Select 注解内嵌 SQL，无需 XML 映射文件
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 汇总摘要：总考试人次、最高分、平均分
     */
    @Select("SELECT " +
            "  COUNT(*)                              AS totalExams, " +
            "  COALESCE(MAX(total_score),  0)        AS maxScore,  " +
            "  COALESCE(ROUND(AVG(total_score), 1), 0) AS avgScore  " +
            "FROM exam_record " +
            "WHERE deleted = 0 AND status = 1 AND total_score IS NOT NULL")
    Map<String, Object> getSummary();

    /**
     * 题库数量
     */
    @Select("SELECT COUNT(*) FROM question_bank WHERE deleted = 0")
    Long countQuestions();

    /**
     * 试卷数量
     */
    @Select("SELECT COUNT(*) FROM exam_paper WHERE deleted = 0")
    Long countPapers();

    /**
     * 成绩分布：按 <60 / 60-70 / 70-80 / 80-90 / ≥90 分段统计人数
     */
    @Select("SELECT " +
            "  SUM(CASE WHEN total_score < 60                          THEN 1 ELSE 0 END) AS below60, " +
            "  SUM(CASE WHEN total_score >= 60 AND total_score < 70    THEN 1 ELSE 0 END) AS s6070,   " +
            "  SUM(CASE WHEN total_score >= 70 AND total_score < 80    THEN 1 ELSE 0 END) AS s7080,   " +
            "  SUM(CASE WHEN total_score >= 80 AND total_score < 90    THEN 1 ELSE 0 END) AS s8090,   " +
            "  SUM(CASE WHEN total_score >= 90                         THEN 1 ELSE 0 END) AS above90  " +
            "FROM exam_record " +
            "WHERE deleted = 0 AND status = 1 AND total_score IS NOT NULL")
    Map<String, Object> getScoreDistribution();

    /**
     * 及格率：统计 ≥60 及格人数与 <60 不及格人数
     */
    @Select("SELECT " +
            "  SUM(CASE WHEN total_score >= 60 THEN 1 ELSE 0 END) AS passed, " +
            "  SUM(CASE WHEN total_score <  60 THEN 1 ELSE 0 END) AS failed  " +
            "FROM exam_record " +
            "WHERE deleted = 0 AND status = 1 AND total_score IS NOT NULL")
    Map<String, Object> getPassRate();

    /**
     * 各科目平均分：关联 exam_paper，按 subject_name 分组
     */
    @Select("SELECT " +
            "  p.subject_name            AS subjectName, " +
            "  ROUND(AVG(r.total_score), 1) AS avgScore    " +
            "FROM exam_record r " +
            "INNER JOIN exam_paper p ON r.paper_id = p.id " +
            "WHERE r.deleted = 0 AND r.status = 1 AND r.total_score IS NOT NULL " +
            "  AND p.deleted = 0 " +
            "GROUP BY p.subject_name " +
            "ORDER BY p.subject_name")
    List<Map<String, Object>> getSubjectAvgScore();
}
