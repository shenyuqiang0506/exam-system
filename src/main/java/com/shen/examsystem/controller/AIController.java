package com.shen.examsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.dto.AIGradeRequest;
import com.shen.examsystem.dto.AIGradeResponse;
import com.shen.examsystem.dto.AIGenerateQuestionRequest;
import com.shen.examsystem.dto.AIGenerateQuestionResponse;
import com.shen.examsystem.dto.AISaveQuestionRequest;
import com.shen.examsystem.entity.AIGradeLog;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.interceptor.RequireRole;
import com.shen.examsystem.service.AIService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI控制器 - DeepSeek
 */
@RestController
@RequestMapping("/api/ai")
@RequireRole({1, 2})  // 教师和管理员可用
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * AI主观题判分
     * 
     * @param request 判分请求
     * @return AI判分结果
     */
    @PostMapping("/grade")
    public Result<AIGradeResponse> gradeSubjective(@RequestBody AIGradeRequest request) {
        AIGradeResponse response = aiService.gradeSubjective(
                request.getQuestion(),
                request.getStandardAnswer(),
                request.getStudentAnswer(),
                request.getFullScore()
        );
        return Result.success(response);
    }

    /**
     * AI智能出题
     * 
     * @param request 出题请求
     * @return 生成的题目列表
     */
    @PostMapping("/generate-questions")
    public Result<List<AIGenerateQuestionResponse>> generateQuestions(
            @RequestBody AIGenerateQuestionRequest request) {
        List<AIGenerateQuestionResponse> questions = aiService.generateQuestions(
                request.getSubject(),
                request.getKnowledge(),
                request.getType(),
                request.getDifficulty(),
                request.getCount()
        );
        return Result.success(questions);
    }
    
    /**
     * 保存单个AI生成的题目到题库
     * 
     * @param request 题目信息
     * @param httpRequest HTTP请求
     * @return 保存后的题目
     */
    @PostMapping("/save-question")
    public Result<QuestionBank> saveQuestion(@RequestBody AISaveQuestionRequest request,
                                              HttpServletRequest httpRequest) {
        Long teacherId = (Long) httpRequest.getAttribute("userId");
        QuestionBank question = aiService.saveQuestion(request, teacherId);
        return Result.success(question);
    }
    
    /**
     * 批量保存AI生成的题目到题库
     * 
     * @param params 包含questions列表、subjectName、type
     * @param httpRequest HTTP请求
     * @return 保存的题目数量
     */
    @PostMapping("/save-questions")
    public Result<Integer> saveQuestions(@RequestBody Map<String, Object> params,
                                          HttpServletRequest httpRequest) {
        Long teacherId = (Long) httpRequest.getAttribute("userId");
        
        String subjectName = (String) params.get("subjectName");
        Integer type = (Integer) params.get("type");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questionMaps = (List<Map<String, Object>>) params.get("questions");
        
        List<AIGenerateQuestionResponse> questions = questionMaps.stream().map(map -> {
            AIGenerateQuestionResponse q = new AIGenerateQuestionResponse();
            q.setContent((String) map.get("content"));
            q.setAnswer((String) map.get("answer"));
            q.setAnalysis((String) map.get("analysis"));
            q.setType(type);
            
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) map.get("options");
            q.setOptions(options);
            
            return q;
        }).collect(java.util.stream.Collectors.toList());
        
        int count = aiService.saveQuestions(questions, subjectName, type, teacherId);
        return Result.success(count);
    }

    /**
     * 查询AI判分日志列表
     * 
     * @param page 页码
     * @param size 每页数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    @GetMapping("/logs")
    public Result<IPage<AIGradeLog>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        IPage<AIGradeLog> logs = aiService.getGradeLogs(page, size, startTime, endTime);
        return Result.success(logs);
    }

    /**
     * 查询AI判分日志详情
     * 
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/logs/{id}")
    public Result<AIGradeLog> getLogById(@PathVariable Long id) {
        AIGradeLog log = aiService.getGradeLogById(id);
        return Result.success(log);
    }
}
