package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shen.examsystem.config.AIConfig;
import com.shen.examsystem.dto.AIGradeResponse;
import com.shen.examsystem.dto.AIGenerateQuestionResponse;
import com.shen.examsystem.dto.AISaveQuestionRequest;
import com.shen.examsystem.entity.AIGradeLog;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.mapper.AIGradeLogMapper;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.service.AIService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI服务实现类 - DeepSeek
 */
@Service
@Slf4j
public class AIServiceImpl implements AIService {

    @Autowired
    private AIConfig aiConfig;
    
    @Autowired
    private AIGradeLogMapper aiGradeLogMapper;
    
    @Autowired
    private QuestionBankMapper questionBankMapper;
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    private final Gson gson = new Gson();

    @Override
    public AIGradeResponse gradeSubjective(String question, String standardAnswer,
                                            String studentAnswer, double fullScore) {
        // 1. 构造Prompt
        String prompt = buildGradePrompt(question, standardAnswer, studentAnswer, fullScore);
        
        // 2. 调用MiMo API
        String response = callMiMo(prompt);
        
        // 3. 解析响应
        AIGradeResponse gradeResponse = parseGradeResponse(response, fullScore);
        
        // 4. 记录判分日志
        saveGradeLog(null, null, studentAnswer, standardAnswer, gradeResponse);
        
        return gradeResponse;
    }

    @Override
    public List<AIGenerateQuestionResponse> generateQuestions(String subject, String knowledge,
                                                               Integer type, Integer difficulty,
                                                               Integer count) {
        // 1. 构造Prompt
        String prompt = buildGeneratePrompt(subject, knowledge, type, difficulty, count);
        
        // 2. 调用MiMo API
        String response = callMiMo(prompt);
        
        // 3. 解析响应
        return parseGenerateResponse(response, type);
    }
    
    @Override
    public IPage<AIGradeLog> getGradeLogs(Integer page, Integer size, 
                                           LocalDateTime startTime, LocalDateTime endTime) {
        Page<AIGradeLog> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<AIGradeLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围查询
        if (startTime != null) {
            wrapper.ge(AIGradeLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AIGradeLog::getCreateTime, endTime);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(AIGradeLog::getCreateTime);
        
        return aiGradeLogMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public AIGradeLog getGradeLogById(Long id) {
        AIGradeLog log = aiGradeLogMapper.selectById(id);
        if (log == null) {
            throw new RuntimeException("日志不存在");
        }
        return log;
    }
    
    @Override
    public QuestionBank saveQuestion(AISaveQuestionRequest request, Long teacherId) {
        QuestionBank question = new QuestionBank();
        question.setTeacherId(teacherId);
        question.setSubjectName(request.getSubjectName());
        question.setType(request.getType());
        question.setStandardAnswer(request.getStandardAnswer());
        question.setScore(request.getScore() != null ? request.getScore() : new BigDecimal("5.0"));
        question.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : new BigDecimal("0.5"));
        
        // 构造content JSON (包含题目和选项)
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("title", request.getContent());
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            List<Map<String, String>> optionsList = new ArrayList<>();
            for (int i = 0; i < request.getOptions().size(); i++) {
                Map<String, String> opt = new HashMap<>();
                opt.put("value", String.valueOf((char)('A' + i)));
                opt.put("label", request.getOptions().get(i));
                optionsList.add(opt);
            }
            contentMap.put("options", optionsList);
        }
        question.setContent(gson.toJson(contentMap));
        
        // 设置给分关键词（如果有解析）
        if (request.getAnalysis() != null) {
            question.setPointsKeyword(request.getAnalysis());
        }
        
        questionBankMapper.insert(question);
        log.info("AI题目保存成功，ID: {}", question.getId());
        
        return question;
    }
    
    @Override
    public int saveQuestions(List<AIGenerateQuestionResponse> questions, String subjectName, 
                             Integer type, Long teacherId) {
        int count = 0;
        for (AIGenerateQuestionResponse q : questions) {
            try {
                AISaveQuestionRequest request = new AISaveQuestionRequest();
                request.setSubjectName(subjectName);
                request.setType(type);
                request.setContent(q.getContent());
                request.setOptions(q.getOptions());
                request.setStandardAnswer(q.getAnswer());
                request.setAnalysis(q.getAnalysis());
                request.setScore(new BigDecimal("5.0"));
                request.setDifficulty(new BigDecimal("0.5"));
                
                saveQuestion(request, teacherId);
                count++;
            } catch (Exception e) {
                log.error("保存题目失败: {}", q.getContent(), e);
            }
        }
        return count;
    }

    /**
     * 构造判分Prompt
     */
    private String buildGradePrompt(String question, String standardAnswer,
                                     String studentAnswer, double fullScore) {
        return String.format("""
                你是一位专业的考试阅卷老师。请根据以下信息对学生答案进行评分。
                
                【题目】%s
                【标准答案】%s
                【学生答案】%s
                【满分】%.1f分
                
                请按以下JSON格式返回：
                {
                    "score": 得分(0到%.1f之间的数字),
                    "reason": "判分依据(详细说明给分原因)",
                    "keywords": ["学生答对的关键点1", "关键点2"],
                    "missing": ["学生遗漏的关键点1", "关键点2"]
                }
                
                注意：
                1. 只返回JSON，不要有其他内容
                2. 得分保留1位小数
                3. 判分依据要具体明确
                """, question, standardAnswer, studentAnswer, fullScore, fullScore);
    }

    /**
     * 构造出题Prompt
     */
    private String buildGeneratePrompt(String subject, String knowledge,
                                        Integer type, Integer difficulty, Integer count) {
        String typeName = switch (type) {
            case 1 -> "单选题（4个选项，1个正确答案）";
            case 2 -> "多选题（4个选项，2-4个正确答案）";
            case 3 -> "判断题（正确/错误）";
            case 4 -> "主观题（需要文字作答）";
            default -> "单选题";
        };

        return String.format("""
                你是一位专业的出题老师。请根据以下要求生成%d道题目。
                
                【科目】%s
                【知识点】%s
                【题型】%s
                【难度】%d级（1最简单，5最难）
                
                请按以下JSON格式返回：
                {
                    "questions": [
                        {
                            "content": "题目内容",
                            "options": ["选项A", "选项B", "选项C", "选项D"],
                            "answer": "正确答案",
                            "analysis": "解析"
                        }
                    ]
                }
                
                注意：
                1. 只返回JSON，不要有其他内容
                2. 单选题answer为单个字母（如"A"）
                3. 多选题answer为多个字母（如"ABC"）
                4. 判断题options为["正确","错误"]，answer为"对"或"错"
                5. 主观题options为空数组，answer为参考答案
                """, count, subject, knowledge, typeName, difficulty);
    }

    /**
     * 调用小米MiMo API (兼容OpenAI格式)
     */
    private String callMiMo(String prompt) {
        try {
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("max_tokens", aiConfig.getMaxTokens());
            requestBody.put("temperature", aiConfig.getTemperature());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            requestBody.put("messages", messages);

            String json = gson.toJson(requestBody);
            
            log.info("调用MiMo API，模型: {}", aiConfig.getModel());

            Request request = new Request.Builder()
                    .url(aiConfig.getBaseUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("MiMo API调用失败: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("AI API调用失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.info("MiMo API响应成功");
                
                // 解析响应获取内容
                Map<String, Object> result = gson.fromJson(responseBody, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            log.error("调用MiMo API失败", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 解析判分响应
     */
    private AIGradeResponse parseGradeResponse(String response, double fullScore) {
        try {
            // 提取JSON部分
            String json = extractJson(response);
            Map<String, Object> result = gson.fromJson(json, 
                    new TypeToken<Map<String, Object>>(){}.getType());
            
            AIGradeResponse gradeResponse = new AIGradeResponse();
            gradeResponse.setScore(((Number) result.get("score")).doubleValue());
            gradeResponse.setReason((String) result.get("reason"));
            
            // 处理列表类型
            Object keywordsObj = result.get("keywords");
            if (keywordsObj instanceof List) {
                gradeResponse.setKeywords((List<String>) keywordsObj);
            }
            
            Object missingObj = result.get("missing");
            if (missingObj instanceof List) {
                gradeResponse.setMissing((List<String>) missingObj);
            }
            
            return gradeResponse;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            throw new RuntimeException("AI响应解析失败");
        }
    }

    /**
     * 解析出题响应
     */
    private List<AIGenerateQuestionResponse> parseGenerateResponse(String response, Integer type) {
        try {
            String json = extractJson(response);
            Map<String, Object> result = gson.fromJson(json, 
                    new TypeToken<Map<String, Object>>(){}.getType());
            List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
            
            return questions.stream().map(q -> {
                AIGenerateQuestionResponse question = new AIGenerateQuestionResponse();
                question.setContent((String) q.get("content"));
                
                // 处理选项列表
                Object optionsObj = q.get("options");
                if (optionsObj instanceof List) {
                    question.setOptions((List<String>) optionsObj);
                } else {
                    question.setOptions(new ArrayList<>());
                }
                
                question.setAnswer((String) q.get("answer"));
                question.setAnalysis((String) q.get("analysis"));
                question.setType(type);
                return question;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            throw new RuntimeException("AI响应解析失败");
        }
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJson(String response) {
        // 去除可能的markdown代码块标记
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
    
    /**
     * 保存判分日志
     */
    private void saveGradeLog(Long recordId, Long questionId, String studentAnswer, 
                               String standardAnswer, AIGradeResponse gradeResponse) {
        try {
            AIGradeLog logEntity = new AIGradeLog();
            logEntity.setRecordId(recordId);
            logEntity.setQuestionId(questionId);
            logEntity.setStudentAnswer(studentAnswer);
            logEntity.setStandardAnswer(standardAnswer);
            logEntity.setAiScore(gradeResponse.getScore());
            logEntity.setAiReason(gradeResponse.getReason());
            logEntity.setModel(aiConfig.getModel());
            
            // 将列表转为JSON字符串
            if (gradeResponse.getKeywords() != null) {
                logEntity.setKeywords(gson.toJson(gradeResponse.getKeywords()));
            }
            if (gradeResponse.getMissing() != null) {
                logEntity.setMissing(gson.toJson(gradeResponse.getMissing()));
            }
            
            aiGradeLogMapper.insert(logEntity);
            log.info("AI判分日志保存成功，得分: {}", gradeResponse.getScore());
        } catch (Exception e) {
            log.error("保存AI判分日志失败", e);
            // 日志保存失败不影响主流程
        }
    }
}
