package com.aicodingmentor.duel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Zero-Cost Real-Time WebSocket STOMP Message Broker.
 * Enables zero-subscription instant duplex messaging for 1v1 Code Duels and Contest Leaderboards.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Free, zero-cost in-memory pub-sub broker
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback endpoint for web clients
        registry.addEndpoint("/ws-duel")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint for modern browsers and low-overhead connections
        registry.addEndpoint("/ws-duel-native")
                .setAllowedOriginPatterns("*");
    }
}
