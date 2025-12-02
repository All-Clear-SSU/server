package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.WifiDetectionRecordDto;
import opensource.project.service.WifiDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WiFi 센서 탐지 기록 조회 컨트롤러
 * WiFi 센서로 탐지된 Detection 기록을 조회하는 API를 제공함
 *
 * 주요 기능:
 * - 센서별 최근 N개 탐지 기록 조회 (그래프 초기 데이터용)
 */
@Slf4j
@RestController
@RequestMapping("/wifi-detections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "WiFi Detection", description = "WiFi 센서 탐지 기록 조회 API")
public class WifiDetectionController {

    private final WifiDetectionService wifiDetectionService;

    /**
     * 특정 WiFi 센서의 최근 N개 탐지 기록을 조회함
     * 페이지 로드 시 그래프를 초기화하는 데 사용됨
     *
     * @param sensorId WiFi 센서 코드 (예: "ESP32-001")
     * @param limit 조회할 최대 개수 (기본값: 50, 최대값: 200)
     * @return 최근 탐지 기록 목록 (최신순)
     */
    @Operation(
            summary = "센서별 최근 탐지 기록 조회",
            description = "특정 WiFi 센서의 최근 N개 탐지 기록을 조회합니다. " +
                    "페이지 로드 시 그래프 초기 데이터를 가져오는 데 사용됩니다. " +
                    "실시간 데이터는 WebSocket(/topic/wifi-sensor/{sensorId}/signal)으로 수신합니다."
    )
    @GetMapping("/sensor/{sensorId}/recent")
    public ResponseEntity<List<WifiDetectionRecordDto>> getRecentDetections(
            @Parameter(description = "WiFi 센서 코드 (예: ESP32-001)", required = true)
            @PathVariable String sensorId,

            @Parameter(description = "조회할 최대 개수 (기본값: 50, 최대값: 200)")
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        log.info("=== WiFi 센서 최근 탐지 기록 조회 API 호출 ===");
        log.info("센서 코드: {}, limit: {}", sensorId, limit);

        try {
            // WiFi 센서의 최근 탐지 기록을 조회함
            List<WifiDetectionRecordDto> detections = wifiDetectionService.getRecentDetections(sensorId, limit);

            log.info("=== WiFi 센서 탐지 기록 조회 성공 ===");
            log.info("센서 코드: {}, 조회된 개수: {}", sensorId, detections.size());

            return ResponseEntity.ok(detections);

        } catch (IllegalArgumentException e) {
            // WiFi 센서를 찾을 수 없는 경우
            log.error("WiFi 센서 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            // 기타 예외 발생 시
            log.error("WiFi 센서 탐지 기록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}