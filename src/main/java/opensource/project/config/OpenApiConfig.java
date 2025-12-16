package opensource.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI 설정 파일
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("All-Clear 프로젝트의 백엔드 API")
                        .version("1.0")
                        .description("All-Clear 프로젝트의 백엔드 REST API 문서")
                        .contact(new Contact()
                                .name("All-Clear Team")
                                .email("hwkwon1@naver.com")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all-apis")
                .pathsToMatch("/**")
                .pathsToExclude("/ws/**", "/topic/**", "/app/**") // WebSocket 엔드포인트 제외
                .build();
    }

    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("1. Member API")
                .pathsToMatch("/members/**")
                .build();
    }

    @Bean
    public GroupedOpenApi survivorApi() {
        return GroupedOpenApi.builder()
                .group("2. Survivor API")
                .pathsToMatch("/survivors/**")
                .build();
    }

    @Bean
    public GroupedOpenApi locationApi() {
        return GroupedOpenApi.builder()
                .group("3. Location API")
                .pathsToMatch("/locations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi detectionApi() {
        return GroupedOpenApi.builder()
                .group("4. Detection API")
                .pathsToMatch("/detections/**")
                .build();
    }

    @Bean
    public GroupedOpenApi cctvApi() {
        return GroupedOpenApi.builder()
                .group("5. CCTV API")
                .pathsToMatch("/cctvs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi wifiSensorApi() {
        return GroupedOpenApi.builder()
                .group("6. WiFi Sensor API")
                .pathsToMatch("/wifi-sensors/**")
                .build();
    }

    @Bean
    public GroupedOpenApi priorityApi() {
        return GroupedOpenApi.builder()
                .group("7. Priority Assessment API")
                .pathsToMatch("/priority-assessments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi websockettestApi() {
        return GroupedOpenApi.builder()
                .group("8. WebSocketTest API")
                .pathsToMatch("/websocket/test/**")
                .build();
    }

    @Bean
    public GroupedOpenApi videoAnalysisApi() {
        return GroupedOpenApi.builder()
                .group("9. Video-Analysis API")
                .pathsToMatch("/video-analysis/**")
                .build();
    }

        @Bean
        public GroupedOpenApi liveStreamApi() {
            return GroupedOpenApi.builder()
                    .group("10. Live-Stream API")
                    .pathsToMatch("/live-stream/**")
                    .build();
        }

    @Bean
    public GroupedOpenApi wifiSensorTestApi() {
        return GroupedOpenApi.builder()
                .group("11. WiFi-Sensor Test API")
                .pathsToMatch("/test/wifi-sensor/**")
                .build();
    }

    @Bean
    public GroupedOpenApi buildingApi() {
        return GroupedOpenApi.builder()
                .group("12. Building API")
                .pathsToMatch("/buildings/**")
                .build();
    }

    @Bean
    public GroupedOpenApi recentSurvivorRecordApi() {
        return GroupedOpenApi.builder()
                .group("13. Recent Survivor Record API")
                .pathsToMatch("/recent-survivors/**")
                .build();
    }

    @Bean
    public GroupedOpenApi wifiDetectionApi() {
        return GroupedOpenApi.builder()
                .group("14. WiFi Detection API")
                .pathsToMatch("/wifi-detections/**")
                .build();
    }
}
