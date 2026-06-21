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
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
                4. reason字段必须是单行文本，不能包含换行符，用空格分隔句子
                5. 所有字符串中的双引号必须用反斜杠转义，如\\"example\\"
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
                            "options": ["选项内容1", "选项内容2", "选项内容3", "选项内容4"],
                            "answer": "正确答案",
                            "analysis": "解析"
                        }
                    ]
                }
                
                注意：
                1. 只返回JSON，不要有其他内容
                2. 选项内容不要带A、B、C、D前缀，直接写选项文字
                3. 单选题answer为单个字母（如"A"）
                4. 多选题answer为多个字母（如"ABC"）
                5. 判断题options为["正确","错误"]，answer为"对"或"错"
                6. 主观题options为空数组，answer为参考答案
                7. analysis字段必须是单行文本，不能包含换行符，用空格分隔句子
                8. 所有字符串中的双引号必须用反斜杠转义，如\\"example\\"
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
            
            String requestUrl = aiConfig.getBaseUrl() + "/chat/completions";
            log.info("=== 开始调用小米MiMo API ===");
            log.info("请求URL: {}", requestUrl);
            log.info("请求模型: {}", aiConfig.getModel());
            log.info("请求参数: {}", json);

            // 小米MiMo API使用api-key头认证
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .addHeader("api-key", aiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            log.info("发送HTTP请求...");
            long startTime = System.currentTimeMillis();
            
            try (Response response = httpClient.newCall(request).execute()) {
                long endTime = System.currentTimeMillis();
                log.info("收到HTTP响应，耗时: {}ms", endTime - startTime);
                log.info("响应状态码: {}", response.code());
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("小米MiMo API调用失败: {} - {}", response.code(), errorBody);
                    
                    // 根据状态码提供友好的错误信息
                    String errorMessage;
                    switch (response.code()) {
                        case 401:
                            errorMessage = "AI服务认证失败，请检查API密钥配置";
                            break;
                        case 403:
                            errorMessage = "AI服务访问被拒绝，请检查API密钥权限";
                            break;
                        case 429:
                            errorMessage = "AI服务请求过于频繁，请稍后再试";
                            break;
                        case 500:
                        case 502:
                        case 503:
                            errorMessage = "AI服务暂时不可用，请稍后再试";
                            break;
                        default:
                            errorMessage = "AI服务调用失败，错误码: " + response.code();
                    }
                    throw new RuntimeException(errorMessage);
                }
                
                String responseBody = response.body().string();
                log.info("小米MiMo API响应成功");
                log.info("API原始响应: {}", responseBody);  // 添加日志
                
                // 解析响应获取内容
                Map<String, Object> result = gson.fromJson(responseBody, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                if (choices == null || choices.isEmpty()) {
                    log.error("响应中没有choices字段，原始响应: {}", responseBody);
                    throw new RuntimeException("AI服务返回的数据格式异常");
                }
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message == null) {
                    log.error("响应中没有message字段，choices: {}", choices);
                    throw new RuntimeException("AI服务返回的消息内容为空");
                }
                String content = (String) message.get("content");
                log.info("AI返回内容: {}", content);  // 添加日志
                return content;
            }
        } catch (java.net.ConnectException e) {
            log.error("=== 网络连接被拒绝 ===");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            throw new RuntimeException("网络连接失败，请检查网络配置或稍后再试");
        } catch (java.net.SocketTimeoutException e) {
            log.error("=== 网络连接超时 ===");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            throw new RuntimeException("网络连接超时，请稍后再试");
        } catch (java.net.UnknownHostException e) {
            log.error("=== DNS解析失败 ===");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            throw new RuntimeException("网络连接失败，请检查网络配置");
        } catch (javax.net.ssl.SSLException e) {
            log.error("=== SSL/TLS握手失败 ===");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            throw new RuntimeException("网络安全连接失败，请检查网络配置");
        } catch (Exception e) {
            log.error("=== 调用小米MiMo API失败 ===");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            log.error("异常堆栈:", e);
            // 如果已经是RuntimeException，直接抛出
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
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
            // 修复JSON中的特殊字符
            json = fixJson(json);
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
     * 解析出题响应 - 使用逐题提取方式，避免整体JSON解析失败
     */
    private List<AIGenerateQuestionResponse> parseGenerateResponse(String response, Integer type) {
        try {
            String json = extractJson(response);
            json = fixJson(json);
            
            // 使用正则逐题提取，避免整体JSON解析失败
            List<AIGenerateQuestionResponse> questions = new ArrayList<>();
            
            // 使用正则匹配每个题目的各个字段
            java.util.regex.Pattern contentPattern = java.util.regex.Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            java.util.regex.Pattern answerPattern = java.util.regex.Pattern.compile("\"answer\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            java.util.regex.Pattern analysisPattern = java.util.regex.Pattern.compile("\"analysis\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            
            // 先尝试整体解析
            try {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(json));
                reader.setLenient(true);
                Map<String, Object> result = gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                List<Map<String, Object>> qList = (List<Map<String, Object>>) result.get("questions");
                if (qList != null) {
                    for (Map<String, Object> q : qList) {
                        AIGenerateQuestionResponse question = new AIGenerateQuestionResponse();
                        question.setContent((String) q.get("content"));
                        Object optionsObj = q.get("options");
                        if (optionsObj instanceof List) {
                            question.setOptions((List<String>) optionsObj);
                        } else {
                            question.setOptions(new ArrayList<>());
                        }
                        question.setAnswer((String) q.get("answer"));
                        question.setAnalysis((String) q.get("analysis"));
                        question.setType(type);
                        questions.add(question);
                    }
                    if (!questions.isEmpty()) {
                        return questions;
                    }
                }
            } catch (Exception e) {
                log.warn("整体JSON解析失败，尝试逐题提取: {}", e.getMessage());
            }
            
            // 整体解析失败，用正则逐题提取
            String[] blocks = json.split("\"content\"\\s*:");
            for (int i = 1; i < blocks.length; i++) {
                String block = blocks[i];
                try {
                    AIGenerateQuestionResponse question = new AIGenerateQuestionResponse();
                    
                    // 提取content
                    java.util.regex.Matcher cm = java.util.regex.Pattern.compile("^\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(block);
                    question.setContent(cm.find() ? cm.group(1) : "");
                    
                    // 提取options
                    List<String> options = new ArrayList<>();
                    int optStart = block.indexOf("[");
                    int optEnd = block.indexOf("]");
                    if (optStart >= 0 && optEnd > optStart) {
                        String optStr = block.substring(optStart + 1, optEnd);
                        java.util.regex.Matcher optMatcher = java.util.regex.Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(optStr);
                        while (optMatcher.find()) {
                            options.add(optMatcher.group(1));
                        }
                    }
                    question.setOptions(options);
                    
                    // 提取answer
                    java.util.regex.Matcher am = answerPattern.matcher(block);
                    question.setAnswer(am.find() ? am.group(1) : "");
                    
                    // 提取analysis - 取最后一个匹配，因为analysis可能很长
                    java.util.regex.Matcher anam = analysisPattern.matcher(block);
                    String analysis = "";
                    while (anam.find()) {
                        analysis = anam.group(1);
                    }
                    question.setAnalysis(analysis);
                    
                    question.setType(type);
                    questions.add(question);
                } catch (Exception e) {
                    log.warn("解析第{}题失败: {}", i, e.getMessage());
                }
            }
            
            if (questions.isEmpty()) {
                throw new RuntimeException("无法从AI响应中提取题目");
            }
            
            return questions;
        } catch (Exception e) {
            log.error("解析出题响应失败", e);
            throw new RuntimeException("AI响应解析失败: " + e.getMessage());
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
     * 修复JSON中的特殊字符问题
     */
    private String fixJson(String json) {
        // 修复analysis字段中的未转义字符
        // 将换行符替换为空格
        json = json.replaceAll("\\n", " ");
        json = json.replaceAll("\\r", "");
        // 将制表符替换为空格
        json = json.replaceAll("\\t", " ");
        // 将多个空格合并为一个
        json = json.replaceAll("\\s+", " ");
        return json;
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
