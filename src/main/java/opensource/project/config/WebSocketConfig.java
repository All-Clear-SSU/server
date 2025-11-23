package opensource.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버에서 클라이언트로 메시지를 보낼 때 사용하는 prefix
        // 클라이언트는 /topic/... 을 구독하여 메시지를 받음
        registry.enableSimpleBroker("/topic");

        // 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (모든 origin 허용)
                .withSockJS(); // SockJS fallback 지원 (WebSocket 미지원 브라우저 대응)
    }
}