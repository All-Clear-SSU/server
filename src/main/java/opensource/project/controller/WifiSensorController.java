package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;
import opensource.project.service.WifiSensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WiFi 센서 관리 컨트롤러
 * WiFi 센서의 CRUD 기능을 제공함
 */
@RestController
@RequestMapping("/wifi-sensors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "WiFi Sensor", description = "WiFi 센서 관리 API")
public class WifiSensorController {

    private final WifiSensorService wifiSensorService;

    /**
     * WiFi 센서 정보를 등록함
     *
     * @param requestDto WiFi 센서 등록 요청 데이터
     * @return 등록된 WiFi 센서 정보
     */
    @Operation(summary = "WiFi 센서 등록", description = "새로운 WiFi 센서를 등록합니다.")
    @PostMapping
    public ResponseEntity<WifiSensorResponseDto> createWifiSensor(@Valid @RequestBody WifiSensorRequestDto requestDto) {
        WifiSensorResponseDto response = wifiSensorService.createWifiSensor(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [수정] WiFi 센서 목록을 조회함
     * active 쿼리 파라미터로 활성 센서만 필터링할 수 있음
     *
     * @param active 활성 상태 필터 (null: 전체, true: 활성, false: 비활성)
     * @return WiFi 센서 목록
     */
    @Operation(
            summary = "WiFi 센서 목록 조회",
            description = "WiFi 센서 목록을 조회합니다. active 파라미터로 활성 센서만 필터링할 수 있습니다. " +
                    "active=true: 활성 센서만, active=false: 비활성 센서만, active 미지정: 전체 센서"
    )
    @GetMapping
    public ResponseEntity<List<WifiSensorResponseDto>> getAllWifiSensors(
            @Parameter(description = "활성 상태 필터 (true: 활성, false: 비활성, 미지정: 전체)")
            @RequestParam(required = false) Boolean active
    ) {
        List<WifiSensorResponseDto> sensors = wifiSensorService.getAllWifiSensors(active);
        return ResponseEntity.ok(sensors);
    }

    // 특정 WiFi 센서 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<WifiSensorResponseDto> updateWifiSensor(
            @PathVariable Long id,
            @Valid @RequestBody WifiSensorRequestDto requestDto) {
        WifiSensorResponseDto response = wifiSensorService.updateWifiSensor(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 특정 WiFi 센서 정보 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWifiSensor(@PathVariable Long id) {
        wifiSensorService.deleteWifiSensor(id);
        return ResponseEntity.ok("WiFi_Sensor deleted successfully");
    }

}