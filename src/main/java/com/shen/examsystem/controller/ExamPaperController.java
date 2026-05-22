package com.shen.examsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.dto.PaperRuleDTO;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.service.ExamPaperService;
import com.shen.examsystem.service.ExamRecordService;
import com.shen.examsystem.service.GeneticAlgorithmService;
import com.shen.examsystem.service.PaperClassService;
import com.shen.examsystem.interceptor.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷管理 Controller
 */
@RestController
@RequestMapping("/api/paper")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private GeneticAlgorithmService geneticAlgorithmService;

    @Autowired
    private PaperClassService paperClassService;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    /**
     * 分页查询试卷列表（教师）
     */
    @GetMapping("/page")
    @RequireRole({1, 2})  // 教师或管理员
    public Result<Page<ExamPaper>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subjectName) {

        Page<ExamPaper> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        if (subjectName != null && !subjectName.isEmpty()) {
            wrapper.eq(ExamPaper::getSubjectName, subjectName);
        }
        wrapper.orderByDesc(ExamPaper::getCreateTime);
        Page<ExamPaper> result = examPaperService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 智能组卷（遗传算法）
     */
    @PostMapping("/auto-create")
    @Transactional(rollbackFor = Exception.class)
    @RequireRole({1})  // 仅教师
    public Result<Long> autoCreate(@RequestBody PaperRuleDTO rule) {
        // 调试日志
        System.out.println("接收到的组卷规则: title=" + rule.getTitle() 
            + ", startTime=" + rule.getStartTime() 
            + ", endTime=" + rule.getEndTime());
        
        // 1. 查询该科目的所有候选题目
        LambdaQueryWrapper<QuestionBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBank::getSubjectName, rule.getSubjectName());
        List<QuestionBank> allQuestions = questionBankMapper.selectList(queryWrapper);

        // 2. 使用遗传算法生成最优试卷
        List<QuestionBank> selectedQuestions = geneticAlgorithmService.generatePaper(rule, allQuestions);

        // 3. 创建试卷
        ExamPaper paper = new ExamPaper();
        // 使用用户提供的试卷名称，如果为空则使用默认名称
        String title = (rule.getTitle() != null && !rule.getTitle().isEmpty()) 
            ? rule.getTitle() 
            : rule.getSubjectName() + "-智能组卷";
        paper.setTitle(title);
        paper.setSubjectName(rule.getSubjectName());
        paper.setTotalScore(rule.getTotalScore());
        paper.setTargetDifficulty(rule.getTargetDifficulty());
        paper.setStartTime(rule.getStartTime());
        paper.setEndTime(rule.getEndTime());
        paper.setIsArchived(0);
        examPaperService.save(paper);

        // 4. 关联题目
        for (QuestionBank question : selectedQuestions) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(question.getId());
            paperQuestionMapper.insert(pq);
        }

        // 5. 分配班级
        if (rule.getClassIds() != null && !rule.getClassIds().isEmpty()) {
            paperClassService.assignClassesToPaper(paper.getId(), rule.getClassIds());
        }

        return Result.success("智能组卷成功", paper.getId());
    }

    /**
     * 手动组卷保存
     */
    @PostMapping("/manual-create")
    @RequireRole({1})  // 仅教师
    public Result<String> manualCreate(@RequestBody ManualCreateDTO dto) {
        examPaperService.manualCreatePaper(dto.getPaper(), dto.getQuestionIds());

        // 分配班级
        if (dto.getClassIds() != null && !dto.getClassIds().isEmpty()) {
            paperClassService.assignClassesToPaper(dto.getPaper().getId(), dto.getClassIds());
        }

        return Result.success("组卷成功", null);
    }

    /**
     * 试卷归档
     */
    @PutMapping("/archive/{id}")
    @RequireRole({1})  // 仅教师
    public Result<String> archive(@PathVariable Long id) {
        examPaperService.archivePaper(id);
        return Result.success("归档成功", null);
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    @RequireRole({1})  // 仅教师
    public Result<Void> delete(@PathVariable Long id) {
        examPaperService.deletePaper(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新试卷基本信息
     */
    @PutMapping("/{id}")
    @RequireRole({1, 2})  // 教师和管理员
    public Result<Void> update(@PathVariable Long id, @RequestBody ExamPaper paper) {
        paper.setId(id);
        examPaperService.updateById(paper);
        return Result.success("更新成功", null);
    }

    /**
     * 学生获取可考试卷列表
     */
    @GetMapping("/list-active")
    public Result<List<ExamPaper>> listActive() {
        List<ExamPaper> papers = examPaperService.listActivePapers();
        return Result.success(papers);
    }

    /**
     * 学生获取活跃题目集（进行中）
     */
    @GetMapping("/active")
    public Result<List<java.util.Map<String, Object>>> getActivePapers(HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        List<java.util.Map<String, Object>> papers = examPaperService.getActivePapersForStudent(studentId);
        return Result.success(papers);
    }

    /**
     * 学生获取所有题目集（分页）
     */
    @GetMapping("/all")
    public Result<java.util.Map<String, Object>> getAllPapers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        java.util.Map<String, Object> result = examPaperService.getAllPapersForStudent(studentId, page, size);
        return Result.success(result);
    }

    /**
     * 获取试卷详情（包含完整题目信息）
     */
    @GetMapping("/detail/{id}")
    public Result<java.util.Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");

        // 如果是学生角色，检查考试状态
        if (studentId != null) {
            String role = (String) request.getAttribute("role");
            if ("student".equals(role) || "0".equals(role)) {
                // 检查是否已完成考试
                if (examRecordService.hasCompletedExam(studentId, id)) {
                    return Result.error(403, "您已完成该试卷考试，不可重复参加");
                }

                // 检查是否有正在进行的考试
                ExamRecord ongoing = examRecordService.getOngoingExam(studentId, id);
                if (ongoing != null) {
                    java.util.Map<String, Object> res = new java.util.HashMap<>();
                    res.put("paper", examPaperService.getById(id));
                    res.put("ongoingRecord", ongoing);
                    res.put("message", "有正在进行的考试");
                    return Result.success("继续考试", res);
                }
            }
        }

        ExamPaper paper = examPaperService.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 查询试卷中的题目关联
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, id);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        List<Long> questionIds = paperQuestions.stream()
                .map(PaperQuestion::getQuestionId)
                .collect(Collectors.toList());

        // 查询完整题目信息
        List<QuestionBank> questions = new java.util.ArrayList<>();
        for (Long qId : questionIds) {
            QuestionBank q = questionBankMapper.selectById(qId);
            if (q != null) {
                questions.add(q);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("paper", paper);
        result.put("questionIds", questionIds);
        result.put("questions", questions);
        return Result.success(result);
    }

    /**
     * 手动组卷请求体 DTO
     */
    public static class ManualCreateDTO {
        private ExamPaper paper;
        private List<Long> questionIds;
        private List<Long> classIds;

        public ExamPaper getPaper() { return paper; }
        public void setPaper(ExamPaper paper) { this.paper = paper; }
        public List<Long> getQuestionIds() { return questionIds; }
        public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
        public List<Long> getClassIds() { return classIds; }
        public void setClassIds(List<Long> classIds) { this.classIds = classIds; }
    }
}
