package com.shen.examsystem.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * 1. 将 Long/long 序列化为 JSON 字符串，防止前端 JS 丢失精度（雪花ID > Number.MAX_SAFE_INTEGER）
 * 2. 允许前端以字符串形式传回 Long 字段（反序列化 String → Long）
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Long/long 序列化为 String（前端 JS 不会丢失精度）
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // 允许前端将 String 类型的 ID 传回后端并自动转为 Long
            builder.postConfigurer(objectMapper ->
                objectMapper.coercionConfigFor(LogicalType.Integer)
                    .setCoercion(CoercionInputShape.String, CoercionAction.TryConvert)
            );
        };
    }
}
