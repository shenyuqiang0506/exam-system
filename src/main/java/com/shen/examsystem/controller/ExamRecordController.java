package com.shen.examsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.service.ExamRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /**
     * 提交试卷（交卷）
     */
    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody SubmitDTO submitDTO, HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
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
    public Result<Map<String, Object>> detail(@PathVariable Long recordId) {
        Map<String, Object> detail = examRecordService.getRecordDetail(recordId);
        return Result.success(detail);
    }

    /**
     * 查询某试卷的所有成绩列表（教师端）
     */
    @GetMapping("/list/{paperId}")
    public Result<List<ExamRecord>> listByPaperId(@PathVariable Long paperId) {
        List<ExamRecord> records = examRecordService.listByPaperId(paperId);
        return Result.success(records);
    }

    /**
     * 导出成绩到 Excel
     */
    @GetMapping("/export/{paperId}")
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
