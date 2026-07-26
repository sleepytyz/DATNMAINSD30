package com.example.th06876_java202.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket (STOMP + SockJS) — kênh kết nối THỜI GIAN THỰC giữa
 * Website bán hàng online và Website quản lý bán hàng.
 *
 * Các kênh (topic) sử dụng trong hệ thống:
 *  - /topic/quanly/don-hang      : đơn online mới đặt / đổi trạng thái  → màn quản lý đơn hàng, chuông thông báo
 *  - /topic/quanly/ton-kho       : cảnh báo tồn kho (sắp hết / hết hàng) sau khi khách đặt online
 *  - /topic/quanly/module        : trạng thái hoạt động của từng module (phát định kỳ + khi có thay đổi)
 *  - /topic/don-hang/{maHoaDon}  : cập nhật trạng thái 1 đơn cụ thể → trang "Theo dõi đơn hàng" của khách tự đổi
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker đơn giản trong bộ nhớ, đủ cho quy mô 1 cửa hàng
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
