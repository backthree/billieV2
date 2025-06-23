package com.nextdoor.nextdoor.domain.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP over WebSocket 설정: 엔드포인트와 브로커를 등록
 */
@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOriginPatterns = {
            "http://k12e205.p.ssafy.io",
            "http://localhost:3000"
    };

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws-chat")
//                .setAllowedOriginPatterns(
//                        "http://k12e205.p.ssafy.io",
//                        "http://localhost:3000"
//                );
        registry.addEndpoint("/ws-chat")                   // 클라이언트가 연결할 엔드포인트
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();             // SockJS fallback 지원

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { 10_000, 10_000 })
                .setTaskScheduler(heartBeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean(name = "ChatHeartBeatScheduler")
    public ThreadPoolTaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        scheduler.setPoolSize(5);
        scheduler.initialize();
        return scheduler;
    }
}
