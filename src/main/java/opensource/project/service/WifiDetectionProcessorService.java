package opensource.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Detection;
import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import opensource.project.domain.WifiSensor;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.DetectionType;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.MqttWifiDetectionDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.dto.WifiSignalDto;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WiFi 센서로 생존자가 탐지된 경우 생존자 매칭 및 Detection 레코드를 생성하는 서비스
 *
 * 주요 역할:
 * 1. 같은 위치의 최근 생존자를 조회하여 기존 생존자와 매칭을 시도함
 * 2. 매칭되지 않으면 새로운 생존자를 생성함
 * 3. Detection 레코드를 생성하여 DB에 저장함 (DetectionType.WIFI)
 * 4. WebSocket으로 생존자 및 탐지 정보를 실시간 브로드캐스트함
 *
 * 생존자 매칭 로직:
 * - 같은 위치(Location)에서 최근 10분 이내에 WiFi로 탐지된 생존자가 있으면 재사용함
 * - 없으면 새로운 생존자로 등록함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WifiDetectionProcessorService {

    private final SurvivorRepository survivorRepository;
    private final DetectionRepository detectionRepository;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    /**
     * 생존자 매칭 시 사용할 시간 임계값 (분 단위)
     * 이 시간 이내에 같은 위치에서 탐지된 생존자가 있으면 동일 생존자로 판단함
     */
    private static final int TIME_THRESHOLD_MINUTES = 10;

    /**
     * WiFi 센서로 탐지된 생존자를 처리하는 메인 메서드
     * 생존자 매칭, Detection 생성, WebSocket 브로드캐스트를 순차적으로 수행함
     *
     * @param mqttData MQTT로 수신한 WiFi 센서 데이터
     * @param sensor WiFi 센서 엔티티
     * @param location 위치 엔티티
     * @param signalDto WebSocket 브로드캐스트용 신호 데이터 (생존자 정보를 업데이트할 예정)
     */
    @Transactional
    public void processDetection(MqttWifiDetectionDto mqttData,
                                  WifiSensor sensor,
                                  Location location,
                                  WifiSignalDto signalDto) {
        log.info("WiFi 생존자 탐지 처리 시작 - 센서: {}, 위치: {}, 신뢰도: {}",
                sensor.getSensorCode(), location.getFullAddress(), mqttData.getConfidence());

        LocalDateTime now = mqttData.getTimestamp();

        // 1. 기존 생존자를 찾거나 새로 생성함
        Survivor survivor = findOrCreateSurvivor(location, now);
        boolean isNewSurvivor = survivor.getId() == null;

        // 2. 생존자 정보를 업데이트함
        updateSurvivorInfo(survivor, location, now);

        // 3. 생존자를 저장함
        survivor = survivorRepository.save(survivor);

        // 4. WifiSignalDto에 생존자 정보를 설정함 (WebSocket 브로드캐스트용)
        signalDto.setSurvivorInfo(survivor.getId(), formatSurvivorNumber(survivor.getSurvivorNumber()));

        // 5. WebSocket으로 생존자 업데이트 또는 신규 추가를 브로드캐스트함
        if (isNewSurvivor) {
            log.info("새로운 생존자 생성됨 - 생존자 번호: {}, 위치: {}",
                    formatSurvivorNumber(survivor.getSurvivorNumber()), location.getFullAddress());
            webSocketService.broadcastNewSurvivorAdded(SurvivorResponseDto.from(survivor));
        } else {
            log.info("기존 생존자 정보 업데이트 - 생존자 번호: {}, 마지막 탐지: {}",
                    formatSurvivorNumber(survivor.getSurvivorNumber()), now);
            webSocketService.broadcastSurvivorUpdate(survivor.getId(), SurvivorResponseDto.from(survivor));
        }

        // 6. Detection 레코드를 생성하여 DB에 저장함
        Detection detection = createDetection(mqttData, survivor, sensor, location, now);
        Detection savedDetection = detectionRepository.save(detection);

        log.info("Detection 레코드 저장 완료 - Detection ID: {}, 탐지 타입: WIFI", savedDetection.getId());

        // 7. WebSocket으로 탐지 정보를 브로드캐스트함
        DetectionResponseDto detectionDto = DetectionResponseDto.from(savedDetection);
        webSocketService.broadcastDetectionUpdate(survivor.getId(), detectionDto);

        log.info("WiFi 생존자 탐지 처리 완료 - 생존자 ID: {}, Detection ID: {}",
                survivor.getId(), savedDetection.getId());
    }

    /**
     * 같은 위치의 최근 생존자를 찾거나 새로운 생존자를 생성함
     * WiFi 탐지의 경우 정확한 위치 매칭만 수행함 (CCTV의 바운딩박스 매칭과 다름)
     *
     * @param location 탐지 위치
     * @param detectionTime 탐지 시각
     * @return 기존 생존자 또는 새로 생성된 생존자 (아직 DB에 저장되지 않음)
     */
    private Survivor findOrCreateSurvivor(Location location, LocalDateTime detectionTime) {
        LocalDateTime timeThreshold = detectionTime.minusMinutes(TIME_THRESHOLD_MINUTES);

        // 같은 위치에서 최근 N분 이내에 WiFi로 탐지된 활성 생존자를 조회함
        List<Survivor> recentSurvivors = survivorRepository.findByLocationAndLastDetectedAtAfterAndIsActiveTrueOrderByLastDetectedAtDesc(
                location,
                timeThreshold
        );

        // WiFi로 탐지된 생존자만 필터링함 (DetectionMethod.WIFI)
        Survivor matchedSurvivor = recentSurvivors.stream()
                .filter(s -> DetectionMethod.WIFI.equals(s.getDetectionMethod()))
                .findFirst()
                .orElse(null);

        if (matchedSurvivor != null) {
            log.debug("기존 생존자 매칭 성공 - 생존자 ID: {}, 마지막 탐지: {}분 전",
                    matchedSurvivor.getId(),
                    java.time.Duration.between(matchedSurvivor.getLastDetectedAt(), detectionTime).toMinutes());
            return matchedSurvivor;
        }

        // 매칭된 생존자가 없으면 새로운 생존자를 생성함
        log.debug("매칭된 생존자 없음 - 새로운 생존자 생성 예정");
        return Survivor.builder()
                .survivorNumber(generateNextSurvivorNumber())
                .location(location)
                .currentStatus(CurrentStatus.STANDING)  // WiFi 탐지 시 기본 상태 (움직임이 있다고 가정)
                .detectionMethod(DetectionMethod.WIFI)
                .rescueStatus(RescueStatus.WAITING)
                .firstDetectedAt(detectionTime)
                .lastDetectedAt(detectionTime)
                .isActive(true)
                .isFalsePositive(false)
                .build();
    }

    /**
     * 생존자 정보를 업데이트함
     * 기존 생존자의 경우 마지막 탐지 시각만 업데이트하고, 새 생존자는 모든 필드가 이미 설정되어 있음
     *
     * @param survivor 업데이트할 생존자
     * @param location 탐지 위치
     * @param detectionTime 탐지 시각
     */
    private void updateSurvivorInfo(Survivor survivor, Location location, LocalDateTime detectionTime) {
        // 기존 생존자인 경우 (ID가 있음)
        if (survivor.getId() != null) {
            survivor.setLastDetectedAt(detectionTime);
            survivor.setIsActive(true);
            log.debug("기존 생존자 정보 업데이트 - 마지막 탐지 시각: {}", detectionTime);
        } else {
            // 새 생존자인 경우 - Builder로 이미 모든 필드가 설정되어 있으므로 추가 작업 불필요
            log.debug("새 생존자 생성 - 위치: {}, 첫 탐지 시각: {}", location.getFullAddress(), detectionTime);
        }
    }

    /**
     * Detection 엔티티를 생성함
     * WiFi 센서로 탐지된 정보를 Detection 테이블에 저장함
     *
     * @param mqttData MQTT 데이터
     * @param survivor 생존자 엔티티
     * @param sensor WiFi 센서 엔티티
     * @param location 위치 엔티티
     * @param detectionTime 탐지 시각
     * @return 생성된 Detection 엔티티 (아직 DB에 저장되지 않음)
     */
    private Detection createDetection(MqttWifiDetectionDto mqttData,
                                       Survivor survivor,
                                       WifiSensor sensor,
                                       Location location,
                                       LocalDateTime detectionTime) {
        // CSI 분석 데이터를 JSON 문자열로 직렬화함
        String csiAnalysisJson = serializeCsiAnalysisToJson(mqttData);

        // Detection 엔티티를 생성함
        Detection detection = Detection.builder()
                .survivor(survivor)
                .detectionType(DetectionType.WIFI)  // WiFi 센서로 탐지됨
                .cctv(null)  // WiFi 탐지이므로 CCTV는 null
                .wifiSensor(sensor)  // WiFi 센서 FK 설정
                .location(location)
                .detectedAt(detectionTime)
                .detectedStatus(survivor.getCurrentStatus())
                .aiAnalysisResult(csiAnalysisJson)  // CSI 분석 데이터 (JSON)
                .aiModelVersion("WiFi-CSI-AI-v1.0")  // AI 모델 버전
                .confidence(mqttData.getConfidence())
                .signalStrength(mqttData.getSignalStrength())
                .rawData(csiAnalysisJson)  // 원시 데이터 (CSI 분석 결과)
                // CCTV 전용 필드는 null로 설정됨
                .fireCount(null)
                .humanCount(null)
                .smokeCount(null)
                .totalObjects(null)
                .imageUrl(null)
                .videoUrl(null)
                .analyzedImage(null)
                .build();

        log.debug("Detection 엔티티 생성 완료 - 탐지 타입: WIFI, 신뢰도: {}, 신호 강도: {} dBm",
                mqttData.getConfidence(), mqttData.getSignalStrength());

        return detection;
    }

    /**
     * CSI 분석 데이터를 JSON 문자열로 직렬화함
     * Detection 엔티티의 rawData 및 aiAnalysisResult 필드에 저장하기 위해 사용됨
     *
     * @param mqttData MQTT 데이터 (CSI 분석 데이터 포함)
     * @return JSON 문자열 (실패 시 빈 객체 "{}")
     */
    private String serializeCsiAnalysisToJson(MqttWifiDetectionDto mqttData) {
        try {
            if (mqttData.getCsiAnalysis() != null) {
                return objectMapper.writeValueAsString(mqttData.getCsiAnalysis());
            } else {
                log.warn("CSI 분석 데이터가 null입니다. 빈 JSON 객체로 저장합니다.");
                return "{}";
            }
        } catch (JsonProcessingException e) {
            log.error("CSI 분석 데이터 JSON 직렬화 실패: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * 다음 생존자 번호를 생성함
     * 데이터베이스에서 가장 큰 생존자 번호를 조회하여 +1한 값을 반환함
     *
     * @return 다음 생존자 번호 (예: 1, 2, 3, ...)
     */
    private Integer generateNextSurvivorNumber() {
        Integer maxNumber = survivorRepository.findMaxSurvivorNumber();
        int nextNumber = (maxNumber != null) ? maxNumber + 1 : 1;
        log.debug("다음 생존자 번호 생성: {}", nextNumber);
        return nextNumber;
    }

    /**
     * 생존자 번호를 포맷팅함
     * 예: 1 → "S-001", 42 → "S-042"
     *
     * @param survivorNumber 생존자 번호
     * @return 포맷팅된 문자열
     */
    private String formatSurvivorNumber(Integer survivorNumber) {
        return String.format("S-%03d", survivorNumber);
    }
}
