package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Detection;
import opensource.project.domain.WifiSensor;
import opensource.project.domain.enums.DetectionType;
import opensource.project.dto.WifiDetectionRecordDto;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.WifiSensorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WiFi 센서 탐지 기록 조회 서비스
 * Detection 엔티티에서 WiFi 센서로 탐지된 기록을 조회하여 반환함
 *
 * 주요 역할:
 * - 센서별 최근 N개 탐지 기록을 조회함
 * - 페이지 로드 시 그래프 초기 데이터를 제공함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WifiDetectionService {

    private final DetectionRepository detectionRepository;
    private final WifiSensorRepository wifiSensorRepository;

    /**
     * 특정 WiFi 센서의 최근 N개 탐지 기록을 조회함
     * 페이지 로드 시 그래프를 초기화하는 데 사용됨
     *
     * @param sensorId WiFi 센서 ID (데이터베이스 ID, 예: 1, 2, 3)
     * @param limit 조회할 최대 개수 (기본값: 50, 최대값: 200)
     * @return WiFi 탐지 기록 목록 (최신순)
     */
    public List<WifiDetectionRecordDto> getRecentDetections(Long sensorId, Integer limit) {
        log.info("WiFi 센서 최근 탐지 기록 조회 요청 - 센서 ID (DB): {}, limit: {}", sensorId, limit);

        // limit 파라미터 검증 및 기본값 설정을 수행함
        int validatedLimit = validateAndSetLimit(limit);

        // WiFi 센서를 ID로 조회함
        WifiSensor sensor = wifiSensorRepository.findById(sensorId)
                .orElseThrow(() -> new IllegalArgumentException("WiFi 센서를 찾을 수 없습니다. 센서 ID: " + sensorId));

        // Detection 엔티티를 조회함 (DetectionType.WIFI만 조회)
        List<Detection> detections = detectionRepository.findByWifiSensorIdAndDetectionTypeOrderByDetectedAtDesc(
                sensor.getId(),
                DetectionType.WIFI,
                PageRequest.of(0, validatedLimit)
        );

        log.info("WiFi 센서 탐지 기록 조회 완료 - 센서 ID (DB): {}, 조회된 개수: {}", sensorId, detections.size());

        // Detection 엔티티를 WifiDetectionRecordDto로 변환하여 반환함
        return detections.stream()
                .map(WifiDetectionRecordDto::from)
                .collect(Collectors.toList());
    }

    /**
     * limit 파라미터를 검증하고 기본값을 설정함
     * null이거나 범위를 벗어나면 기본값 또는 최대값으로 조정함
     *
     * @param limit 사용자가 요청한 limit 값
     * @return 검증된 limit 값 (1 ~ 200 범위)
     */
    private int validateAndSetLimit(Integer limit) {
        // limit이 null이면 기본값 50을 사용함
        if (limit == null) {
            log.debug("limit 파라미터가 null입니다. 기본값 50을 사용합니다.");
            return 50;
        }

        // limit이 1보다 작으면 1로 조정함
        if (limit < 1) {
            log.warn("limit 파라미터가 1보다 작습니다. 1로 조정합니다. 요청값: {}", limit);
            return 1;
        }

        // limit이 200보다 크면 200으로 제한함 (성능 및 메모리 보호)
        if (limit > 200) {
            log.warn("limit 파라미터가 200을 초과합니다. 200으로 제한합니다. 요청값: {}", limit);
            return 200;
        }

        return limit;
    }
}