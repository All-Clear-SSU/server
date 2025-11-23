package opensource.project.service;

import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;

import java.util.List;

/**
 * WiFi 센서 관리 서비스 인터페이스
 */
public interface WifiSensorService {

    // WiFi 센서 등록
    WifiSensorResponseDto createWifiSensor(WifiSensorRequestDto requestDto);

    // 전체 WiFi 센서 조회
    List<WifiSensorResponseDto> getAllWifiSensors();

    // WiFi 센서 정보 수정
    WifiSensorResponseDto updateWifiSensor(Long id, WifiSensorRequestDto requestDto);

    // WiFi 센서 삭제
    void deleteWifiSensor(Long id);
}