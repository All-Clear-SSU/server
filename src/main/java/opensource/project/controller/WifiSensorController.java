package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;
import opensource.project.service.WifiSensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wifi-sensors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WifiSensorController {

    private final WifiSensorService wifiSensorService;

    // WiFi 센서 정보 등록
    @PostMapping
    public ResponseEntity<WifiSensorResponseDto> createWifiSensor(@Valid @RequestBody WifiSensorRequestDto requestDto) {
        WifiSensorResponseDto response = wifiSensorService.createWifiSensor(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 WiFi 센서 정보 조회
    @GetMapping
    public ResponseEntity<List<WifiSensorResponseDto>> getAllWifiSensors() {
        List<WifiSensorResponseDto> sensors = wifiSensorService.getAllWifiSensors();
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