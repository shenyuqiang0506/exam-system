package com.shen.examsystem.controller;

import com.shen.examsystem.common.Result;
import com.shen.examsystem.mapper.StatisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计 Controller
 * 提供教师端大屏看板所需的聚合统计数据
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsMapper statisticsMapper;


    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        // 1. 汇总摘要（考试人次、最高分、均分）+ 题库/试卷数量
        Map<String, Object> summary = statisticsMapper.getSummary();
        // getSummary 可能因无记录返回空Map，保底处理
        if (summary == null) {
            summary = new HashMap<>();
        }
        summary.put("questionCount", statisticsMapper.countQuestions());
        summary.put("paperCount",    statisticsMapper.countPapers());

        // 2. 成绩分布（五个分数段人数）
        Map<String, Object> scoreDistribution = statisticsMapper.getScoreDistribution();

        // 3. 及格/不及格人数
        Map<String, Object> passRate = statisticsMapper.getPassRate();

        // 4. 各科目平均分
        List<Map<String, Object>> subjectAvg = statisticsMapper.getSubjectAvgScore();

        // 聚合返回
        Map<String, Object> data = new HashMap<>();
        data.put("summary",           summary);
        data.put("scoreDistribution", scoreDistribution);
        data.put("passRate",          passRate);
        data.put("subjectAvg",        subjectAvg);

        return Result.success(data);
    }
}
