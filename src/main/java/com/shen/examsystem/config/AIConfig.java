package com.shen.examsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.siliconflow")
public class AIConfig {
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 最大生成Token数
     */
    private Integer maxTokens;
    
    /**
     * 温度参数 (0-1)
     */
    private Double temperature;
}
