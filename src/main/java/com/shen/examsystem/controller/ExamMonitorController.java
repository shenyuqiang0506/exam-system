package com.shen.examsystem.controller;

import com.shen.examsystem.common.Result;
import com.shen.examsystem.service.ExamMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试监控 Controller
 */
@RestController
@RequestMapping("/api/monitor")
public class ExamMonitorController {

    @Autowired
    private ExamMonitorService examMonitorService;

    /**
     * 获取某试卷的在线学生列表
     */
    @GetMapping("/students/{paperId}")
    public Result<List<ExamMonitorService.StudentStatus>> getOnlineStudents(
            @PathVariable Long paperId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        List<ExamMonitorService.StudentStatus> students = examMonitorService.getOnlineStudents(paperId);
        return Result.success(students);
    }

    /**
     * 获取在线学生数量
     */
    @GetMapping("/count/{paperId}")
    public Result<Integer> getOnlineCount(@PathVariable Long paperId) {
        int count = examMonitorService.getOnlineCount(paperId);
        return Result.success(count);
    }
}
