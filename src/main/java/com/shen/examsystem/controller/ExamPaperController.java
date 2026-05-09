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
import com.shen.examsystem.service.ExamPaperService;
import com.shen.examsystem.service.GeneticAlgorithmService;
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
    private GeneticAlgorithmService geneticAlgorithmService;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    /**
     * 分页查询试卷列表
     */
    @GetMapping("/page")
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
    public Result<Long> autoCreate(@RequestBody PaperRuleDTO rule) {
        // 1. 查询该科目的所有候选题目
        LambdaQueryWrapper<QuestionBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBank::getSubjectName, rule.getSubjectName());
        List<QuestionBank> allQuestions = questionBankMapper.selectList(queryWrapper);

        // 2. 使用遗传算法生成最优试卷
        List<QuestionBank> selectedQuestions = geneticAlgorithmService.generatePaper(rule, allQuestions);

        // 3. 创建试卷
        ExamPaper paper = new ExamPaper();
        paper.setTitle(rule.getSubjectName() + "-智能组卷");
        paper.setSubjectName(rule.getSubjectName());
        paper.setTotalScore(rule.getTotalScore());
        paper.setTargetDifficulty(rule.getTargetDifficulty());
        paper.setIsArchived(0);
        examPaperService.save(paper);

        // 4. 关联题目
        for (QuestionBank question : selectedQuestions) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(question.getId());
            paperQuestionMapper.insert(pq);
        }

        return Result.success("智能组卷成功", paper.getId());
    }

    /**
     * 手动组卷保存
     */
    @PostMapping("/manual-create")
    public Result<String> manualCreate(@RequestBody ManualCreateDTO dto) {
        examPaperService.manualCreatePaper(dto.getPaper(), dto.getQuestionIds());
        return Result.success("组卷成功", null);
    }

    /**
     * 试卷归档
     */
    @PutMapping("/archive/{id}")
    public Result<String> archive(@PathVariable Long id) {
        examPaperService.archivePaper(id);
        return Result.success("归档成功", null);
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
     * 获取试卷详情（包含完整题目信息）
     */
    @GetMapping("/detail/{id}")
    public Result<java.util.Map<String, Object>> detail(@PathVariable Long id) {
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

        public ExamPaper getPaper() { return paper; }
        public void setPaper(ExamPaper paper) { this.paper = paper; }
        public List<Long> getQuestionIds() { return questionIds; }
        public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
    }
}
