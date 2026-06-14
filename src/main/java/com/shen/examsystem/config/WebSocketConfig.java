package com.shen.examsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 启用 STOMP 协议支持实时考试监控
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，客户端订阅的目的地前缀
        config.enableSimpleBroker("/topic");
        // 客户端发送消息的目的地前缀
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 端点，允许跨域
        registry.addEndpoint("/ws/exam")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 支持 SockJS 降级
    }
}
