package com.shen.examsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.interceptor.RequireRole;
import com.shen.examsystem.service.ExamPaperService;
import com.shen.examsystem.service.ExamRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试记录 Controller
 */
@RestController
@RequestMapping("/api/record")
public class ExamRecordController {

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private ExamPaperService examPaperService;

    /**
     * 检查学生对某试卷的考试状态
     */
    @GetMapping("/check/{paperId}")
    public Result<ExamRecord> checkExamStatus(@PathVariable Long paperId, HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        // 查询该学生对该试卷的最新考试记录
        ExamRecord ongoing = examRecordService.getOngoingExam(studentId, paperId);
        if (ongoing != null) {
            return Result.success("有正在进行的考试", ongoing);
        }

        // 检查是否已完成考试
        if (examRecordService.hasCompletedExam(studentId, paperId)) {
            return Result.error(403, "您已完成该试卷考试，不可重复参加");
        }

        return Result.success("可以参加考试", null);
    }

    /**
     * 提交试卷（交卷）
     */
    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody SubmitDTO submitDTO, HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        // 检查是否已交卷
        if (examRecordService.hasCompletedExam(studentId, submitDTO.getPaperId())) {
            return Result.error(403, "您已完成该试卷考试，不可重复交卷");
        }

        // 检查试卷是否已过期
        ExamPaper paper = examPaperService.getById(submitDTO.getPaperId());
        if (paper != null && paper.getEndTime() != null) {
            if (LocalDateTime.now().isAfter(paper.getEndTime())) {
                return Result.error(403, "考试已结束，无法交卷");
            }
        }

        Long recordId = examRecordService.submitPaper(studentId, submitDTO.getPaperId(), submitDTO.getAnswers());
        return Result.success("交卷成功", recordId);
    }

    /**
     * 查询我的考试记录
     */
    @GetMapping("/my-records")
    public Result<List<Map<String, Object>>> myRecords(HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        List<Map<String, Object>> records = examRecordService.getMyRecords(studentId);
        return Result.success(records);
    }

    /**
     * 查询考试成绩详情
     */
    @GetMapping("/detail/{recordId}")
    public Result<Map<String, Object>> detail(@PathVariable Long recordId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        // 获取记录详情
        Map<String, Object> detail = examRecordService.getRecordDetail(recordId);

        // 学生只能查看自己的记录
        if ("student".equals(role) || "0".equals(role)) {
            ExamRecord record = (ExamRecord) detail.get("record");
            if (record != null && !record.getStudentId().equals(userId)) {
                return Result.error(403, "无权查看他人成绩");
            }
        }

        return Result.success(detail);
    }

    /**
     * 查询某试卷的所有成绩列表（教师端）
     */
    @GetMapping("/list/{paperId}")
    @RequireRole({1, 2})  // 教师和管理员
    public Result<List<ExamRecord>> listByPaperId(@PathVariable Long paperId) {
        List<ExamRecord> records = examRecordService.listByPaperId(paperId);
        return Result.success(records);
    }

    /**
     * 强制学生交卷（教师端）
     */
    @PostMapping("/force-submit")
    @RequireRole({1, 2})
    public Result<String> forceSubmit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long studentId = Long.valueOf(params.get("studentId").toString());
        Long paperId = Long.valueOf(params.get("paperId").toString());

        // 查找进行中的考试
        ExamRecord ongoing = examRecordService.getOngoingExam(studentId, paperId);

        // 如果没有进行中的，查找最新的记录
        if (ongoing == null) {
            ongoing = examRecordService.getStudentRecord(studentId, paperId);
        }

        if (ongoing == null) {
            return Result.error(404, "未找到该学生的考试记录");
        }

        // 如果已经交卷，直接返回成功
        if (ongoing.getStatus() == 1) {
            return Result.success("该学生已交卷", null);
        }

        // 将状态改为已交卷
        ongoing.setStatus(1);
        ongoing.setUpdateTime(LocalDateTime.now());
        ongoing.setTotalScore(BigDecimal.ZERO);
        ongoing.setObjectiveScore(BigDecimal.ZERO);
        ongoing.setSubjectiveScore(BigDecimal.ZERO);
        examRecordService.updateById(ongoing);

        return Result.success("强制交卷成功", null);
    }

    /**
     * 导出成绩到 Excel
     */
    @GetMapping("/export/{paperId}")
    @RequireRole({1, 2})  // 教师和管理员
    public void export(@PathVariable Long paperId, HttpServletResponse response) throws Exception {
        List<ExamRecord> records = examRecordService.listByPaperId(paperId);

        List<ExamRecordExportVO> exportList = records.stream()
                .map(r -> {
                    ExamRecordExportVO vo = new ExamRecordExportVO();
                    vo.setStudentId(r.getStudentId());
                    vo.setTotalScore(r.getTotalScore());
                    vo.setObjectiveScore(r.getObjectiveScore());
                    vo.setSubjectiveScore(r.getSubjectiveScore());
                    return vo;
                })
                .collect(Collectors.toList());

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("成绩表", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ExamRecordExportVO.class)
                .sheet("成绩表")
                .doWrite(exportList);
    }

    // ========== 内部 DTO ==========

    public static class SubmitDTO {
        private Long paperId;
        private List<AnswerDTO> answers;

        public Long getPaperId() { return paperId; }
        public void setPaperId(Long paperId) { this.paperId = paperId; }
        public List<AnswerDTO> getAnswers() { return answers; }
        public void setAnswers(List<AnswerDTO> answers) { this.answers = answers; }
    }

    public static class AnswerDTO {
        private Long questionId;
        private String answer;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }

    public static class ExamRecordExportVO {
        @ExcelProperty("学生ID")
        private Long studentId;
        @ExcelProperty("总得分")
        private BigDecimal totalScore;
        @ExcelProperty("客观题得分")
        private BigDecimal objectiveScore;
        @ExcelProperty("主观题得分")
        private BigDecimal subjectiveScore;

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public BigDecimal getTotalScore() { return totalScore; }
        public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
        public BigDecimal getObjectiveScore() { return objectiveScore; }
        public void setObjectiveScore(BigDecimal objectiveScore) { this.objectiveScore = objectiveScore; }
        public BigDecimal getSubjectiveScore() { return subjectiveScore; }
        public void setSubjectiveScore(BigDecimal subjectiveScore) { this.subjectiveScore = subjectiveScore; }
    }
}
