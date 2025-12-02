package opensource.project.service;

import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;

import java.util.List;

/**
 * WiFi 센서 관리 서비스 인터페이스
 */
public interface WifiSensorService {

    /**
     * WiFi 센서를 등록함
     *
     * @param requestDto WiFi 센서 등록 요청 데이터
     * @return 등록된 WiFi 센서 정보
     */
    WifiSensorResponseDto createWifiSensor(WifiSensorRequestDto requestDto);

    /**
     * [수정] WiFi 센서 목록을 조회함
     * active 파라미터로 활성 센서만 필터링할 수 있음
     *
     * @param active 활성 상태 필터 (null: 전체, true: 활성, false: 비활성)
     * @return WiFi 센서 목록
     */
    List<WifiSensorResponseDto> getAllWifiSensors(Boolean active);

    /**
     * WiFi 센서 정보를 수정함
     *
     * @param id 수정할 센서 ID
     * @param requestDto 수정 요청 데이터
     * @return 수정된 WiFi 센서 정보
     */
    WifiSensorResponseDto updateWifiSensor(Long id, WifiSensorRequestDto requestDto);

    /**
     * WiFi 센서를 삭제함
     *
     * @param id 삭제할 센서 ID
     */
    void deleteWifiSensor(Long id);
}