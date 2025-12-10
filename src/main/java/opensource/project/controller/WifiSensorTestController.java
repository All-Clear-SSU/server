package opensource.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Location;
import opensource.project.domain.WifiSensor;
import opensource.project.dto.MqttWifiDetectionDto;
import opensource.project.dto.WifiSignalDto;
import opensource.project.repository.WifiSensorRepository;
import opensource.project.service.WebSocketService;
import opensource.project.service.WifiDetectionProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WiFi 센서 테스트용 Controller
 *
 * 개발/테스트 환경에서만 사용
 *
 * 사용법:
 * 1. 단일 패킷 전송: POST /test/wifi-sensor/{sensorId}/send-mock
 * 2. 연속 패킷 전송: POST /test/wifi-sensor/{sensorId}/start-streaming?count=100&intervalMs=5000
 * 3. 스트리밍 중지: POST /test/wifi-sensor/{sensorId}/stop-streaming
 */
@Slf4j
@RestController
@RequestMapping("/test/wifi-sensor")
@RequiredArgsConstructor
public class WifiSensorTestController {

    private final WebSocketService webSocketService;
    private final WifiDetectionProcessorService wifiDetectionProcessorService;
    private final WifiSensorRepository wifiSensorRepository;
    private final Random random = new Random();

    // 센서별 스트리밍 스케줄러 관리
    private final java.util.concurrent.ConcurrentHashMap<Long, ScheduledExecutorService> streamingTasks =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 단일 Mock WiFi CSI 패킷 전송
     */
    @PostMapping("/{sensorId}/send-mock")
    public ResponseEntity<WifiSignalDto> sendMockSignal(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "false") Boolean survivorDetected) {

        log.info("📡 [테스트] Mock WiFi 신호 전송 - 센서 ID: {}, 생존자 탐지: {}", sensorId, survivorDetected);

        WifiSignalDto mockData = generateMockSignal(sensorId, survivorDetected);

        // 생존자 탐지 시 생존자 생성 및 DB 저장
        if (Boolean.TRUE.equals(survivorDetected)) {
            processSurvivorDetection(sensorId, mockData);
        }

        // WebSocket 브로드캐스트
        webSocketService.broadcastWifiSignal(sensorId, mockData);

        return ResponseEntity.ok(mockData);
    }

    /**
     * 연속 Mock WiFi CSI 패킷 스트리밍 시작
     */
    @PostMapping("/{sensorId}/start-streaming")
    public ResponseEntity<String> startStreaming(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "100") Integer count,
            @RequestParam(defaultValue = "5000") Integer intervalMs,
            @RequestParam(defaultValue = "0.3") Double survivorProbability) {

        // 이미 실행 중인 스트리밍이 있으면 중지
        if (streamingTasks.containsKey(sensorId)) {
            stopStreaming(sensorId);
        }

        log.info("🚀 [테스트] WiFi 신호 스트리밍 시작 - 센서 ID: {}, 패킷: {}개, 간격: {}ms, 생존자 확률: {}%",
                sensorId, count, intervalMs, survivorProbability * 100);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        streamingTasks.put(sensorId, scheduler);

        final int[] sentCount = {0};

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (sentCount[0] >= count) {
                    stopStreaming(sensorId);
                    log.info("✅ [테스트] WiFi 신호 스트리밍 완료 - 센서 ID: {}, 총 {}개 패킷 전송", sensorId, sentCount[0]);
                    return;
                }

                // 생존자 탐지 시뮬레이션
                boolean survivorDetected = random.nextDouble() < survivorProbability;
                WifiSignalDto mockData = generateMockSignal(sensorId, survivorDetected);

                // 생존자 탐지 시 생존자 생성 및 DB 저장
                if (survivorDetected) {
                    processSurvivorDetection(sensorId, mockData);
                    log.info("🚨 [테스트] 생존자 탐지! 센서 ID: {}, 패킷: {}/{}",
                            sensorId, sentCount[0], count);
                }

                // WebSocket 브로드캐스트
                webSocketService.broadcastWifiSignal(sensorId, mockData);

                sentCount[0]++;

            } catch (Exception e) {
                log.error("❌ [테스트] WiFi 신호 전송 실패 - 센서 ID: {}", sensorId, e);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        return ResponseEntity.ok(String.format(
                "🚀 WiFi 신호 스트리밍 시작 - 센서 ID: %d, %d개 패킷, %dms 간격, 생존자 확률 %.0f%%",
                sensorId, count, intervalMs, survivorProbability * 100));
    }

    /**
     * 연속 패킷 스트리밍 중지
     */
    @PostMapping("/{sensorId}/stop-streaming")
    public ResponseEntity<String> stopStreaming(@PathVariable Long sensorId) {
        ScheduledExecutorService scheduler = streamingTasks.remove(sensorId);

        if (scheduler != null) {
            scheduler.shutdownNow();
            log.info("⏹️ [테스트] WiFi 신호 스트리밍 중지 - 센서 ID: {}", sensorId);
            return ResponseEntity.ok("⏹️ WiFi 신호 스트리밍 중지 - 센서 ID: " + sensorId);
        } else {
            return ResponseEntity.ok("ℹ️ 실행 중인 스트리밍이 없습니다 - 센서 ID: " + sensorId);
        }
    }

    /**
     * 모든 스트리밍 중지
     */
    @PostMapping("/stop-all-streaming")
    public ResponseEntity<String> stopAllStreaming() {
        int count = streamingTasks.size();
        streamingTasks.values().forEach(ScheduledExecutorService::shutdownNow);
        streamingTasks.clear();
        log.info("⏹️ [테스트] 모든 WiFi 신호 스트리밍 중지 - {}개 센서", count);
        return ResponseEntity.ok("⏹️ 모든 WiFi 신호 스트리밍 중지 - " + count + "개 센서");
    }

    /**
     * 생존자 탐지 처리
     */
    @Transactional
    protected void processSurvivorDetection(Long sensorId, WifiSignalDto signalDto) {
        try {
            // WiFi 센서 조회 (Location을 함께 조회)
            WifiSensor sensor = wifiSensorRepository.findByIdWithLocation(sensorId)
                    .orElseThrow(() -> new RuntimeException("센서를 찾을 수 없습니다: " + sensorId));

            Location location = sensor.getLocation();

            // MqttWifiDetectionDto 생성 (새로운 3개 필드 형식)
            MqttWifiDetectionDto mqttData = MqttWifiDetectionDto.builder()
                    .sensorId(signalDto.getSensorId())
                    .survivorDetected(signalDto.getSurvivorDetected())
                    .csiAmplitudeSummary(signalDto.getCsiAmplitudeSummary())
                    .build();

            // 타임스탬프 생성
            LocalDateTime timestamp = LocalDateTime.now();

            // 생존자 생성 및 탐지 처리
            wifiDetectionProcessorService.processDetection(mqttData, sensor, location, signalDto, timestamp);

            log.info("✅ [테스트] 생존자 생성 완료 - 센서 ID: {}", sensorId);
        } catch (Exception e) {
            log.error("❌ [테스트] 생존자 생성 실패 - 센서 ID: {}", sensorId, e);
        }
    }

    /**
     * Mock WiFi CSI 데이터 생성
     */
    @Transactional(readOnly = true)
    protected WifiSignalDto generateMockSignal(Long sensorId, Boolean survivorDetected) {
        // 센서의 실제 location 조회
        WifiSensor sensor = wifiSensorRepository.findByIdWithLocation(sensorId)
                .orElseThrow(() -> new RuntimeException("센서를 찾을 수 없습니다: " + sensorId));

        // CSI 진폭 데이터 생성 (34개 부반송파)
        List<Double> csiAmplitudes = new ArrayList<>(34);

        for (int i = 0; i < 34; i++) {
            // 기본 진폭: 10~50 범위
            double amplitude = 10 + random.nextDouble() * 40;

            // 생존자 탐지 시 10~19번 부반송파 진폭 증가 (호흡/움직임 패턴)
            if (survivorDetected && i >= 10 && i < 20) {
                amplitude += random.nextDouble() * 20 + 10; // +10~30 추가

                // 주기적 변동 추가 (호흡 패턴)
                double breathingPattern = Math.sin(System.currentTimeMillis() / 5000.0) * 10;
                amplitude += breathingPattern;
            }

            csiAmplitudes.add(Math.round(amplitude * 10.0) / 10.0); // 소수점 1자리
        }

        // WifiSignalDto 생성 (새로운 형식)
        return WifiSignalDto.builder()
                .sensorId(sensorId)
                .survivorDetected(survivorDetected)
                .csiAmplitudeSummary(csiAmplitudes)
                .timestamp(LocalDateTime.now())
                .build();
    }
}