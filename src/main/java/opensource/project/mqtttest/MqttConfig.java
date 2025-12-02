package opensource.project.mqtttest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.MqttWifiDetectionDto;
import opensource.project.service.WifiDetectionMqttService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT 브로커와의 연결 및 메시지 구독을 설정하는 클래스
 * application.yml의 mqtt.enabled=true로 설정하면 활성화됨
 *
 * 주요 기능:
 * 1. MQTT 브로커에 연결함
 * 2. 지정된 토픽(MQTT_TOPIC)을 구독함
 * 3. ESP32 센서로부터 5초마다 WiFi CSI 신호 데이터를 수신함
 * 4. 수신한 메시지를 WifiDetectionMqttService로 전달하여 비즈니스 로직을 처리함
 *
 * 변경 이력:
 *  handler() 메서드에 WifiDetectionMqttService 연동 추가
 */
@Slf4j
@Configuration
@RequiredArgsConstructor  // [추가] WifiDetectionMqttService 의존성 주입을 위해 추가함
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttConfig {

    // [추가] JSON 파싱을 위한 ObjectMapper를 주입받음
    private final ObjectMapper objectMapper;

    // [추가] MQTT 메시지를 처리할 서비스를 주입받음
    private final WifiDetectionMqttService wifiDetectionMqttService;

    /**
     * MQTT 브로커 URL
     * 환경변수로 주입됨 (예: tcp://mqtt-broker.example.com:1883)
     */
    @Value("${MQTT_BROKER_URL}")
    private String BROKER_URL;

    /**
     * MQTT 클라이언트 ID
     * Spring Boot 애플리케이션을 식별하는 고유 ID
     * 환경변수로 주입됨
     */
    @Value("${MQTT_CLIENT_ID}")
    private String CLIENT_ID;

    /**
     * 구독할 MQTT 토픽
     * ESP32 센서가 메시지를 발행하는 토픽을 지정함
     * 환경변수로 주입됨 (예: wifi/csi/detection)
     */
    @Value("${MQTT_TOPIC}")
    private String TOPIC;

    /**
     * MQTT 클라이언트 팩토리를 생성함
     * MQTT 브로커와의 연결 옵션을 설정함
     *
     * @return DefaultMqttPahoClientFactory 인스턴스
     */
    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions options = new MqttConnectOptions();
        // MQTT 브로커 주소를 설정함
        options.setServerURIs(new String[]{BROKER_URL});
        // 클린 세션을 활성화함 (이전 세션 정보를 유지하지 않음)
        options.setCleanSession(true);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * MQTT 메시지를 수신할 채널을 생성함
     * Spring Integration의 DirectChannel을 사용하여 메시지를 동기적으로 처리함
     *
     * @return MessageChannel 인스턴스
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 메시지를 수신하는 어댑터를 생성함
     * 지정된 토픽을 구독하고 수신한 메시지를 mqttInputChannel로 전달함
     *
     * @return MessageProducer 인스턴스
     */
    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(CLIENT_ID, mqttClientFactory(), TOPIC);
        // 수신한 메시지를 mqttInputChannel로 출력함
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * MQTT 메시지를 처리하는 핸들러를 생성함
     * mqttInputChannel로부터 메시지를 받아서 처리함
     *
     * [변경 전] 단순히 로그만 출력했음:
     *   log.info("MQTT received: {}", payload);
     *
     * [변경 후] WifiDetectionMqttService를 호출하여 비즈니스 로직을 처리함:
     *   1. JSON 페이로드를 MqttWifiDetectionDto로 파싱함
     *   2. WifiDetectionMqttService.processMqttMessage()를 호출함
     *   3. 서비스에서 WebSocket 브로드캐스트 및 DB 저장 수행함
     *
     * @return MessageHandler 인스턴스
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            try {
                // MQTT 메시지의 페이로드를 문자열로 추출함
                String payload = (String) message.getPayload();
                log.info("=== MQTT 메시지 수신 ===");
                log.debug("Raw payload: {}", payload);

                // [변경] JSON 문자열을 MqttWifiDetectionDto 객체로 파싱함
                MqttWifiDetectionDto mqttData = objectMapper.readValue(payload, MqttWifiDetectionDto.class);

                // 센서 ID와 생존자 탐지 여부를 로그에 기록함
                log.info("센서 ID: {}, 생존자 탐지: {}, 신호 강도: {} dBm",
                        mqttData.getSensorId(),
                        mqttData.getSurvivorDetected(),
                        mqttData.getSignalStrength());

                // [변경] WifiDetectionMqttService를 호출하여 비즈니스 로직을 처리함
                // 이 서비스에서 다음 작업을 수행함:
                // 1. 항상: WebSocket으로 실시간 신호 데이터(그래프) 브로드캐스트
                // 2. survivorDetected==true인 경우: 생존자 매칭 및 Detection 레코드 DB 저장
                wifiDetectionMqttService.processMqttMessage(mqttData);

                log.info("=== MQTT 메시지 처리 완료 ===");

            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // JSON 파싱 실패 시 에러 로그를 남김
                log.error("MQTT 메시지 JSON 파싱 실패: {}", e.getMessage(), e);
            } catch (Exception e) {
                // 기타 예외 발생 시 에러 로그를 남김
                log.error("MQTT 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        };
    }
/*
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            String payload = (String) message.getPayload();
            log.info("MQTT received: {}", payload);
        };
    }
    */
}
