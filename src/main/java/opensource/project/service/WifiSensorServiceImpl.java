package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.domain.Location;
import opensource.project.domain.WifiSensor;
import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.WifiSensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WifiSensorServiceImpl implements WifiSensorService {

    private final WifiSensorRepository wifiSensorRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public WifiSensorResponseDto createWifiSensor(WifiSensorRequestDto requestDto) {
        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        // sensorCode 자동 생성: "WIFI-SENSOR-{timestamp}"
        String sensorCode = "WIFI-SENSOR-" + System.currentTimeMillis();

        WifiSensor wifiSensor = WifiSensor.builder()
                .sensorCode(sensorCode)
                .location(location)
                .status(requestDto.getStatus())
                .signalStrength(requestDto.getSignalStrength())
                .detectionRadius(requestDto.getDetectionRadius())
                .isActive(requestDto.getIsActive())
                .lastActiveAt(requestDto.getIsActive() ? LocalDateTime.now() : null)
                .build();

        WifiSensor savedSensor = wifiSensorRepository.save(wifiSensor);
        return WifiSensorResponseDto.from(savedSensor);
    }

    /**
     * [수정] WiFi 센서 목록을 조회함
     * active 파라미터가 null이면 전체 센서를 조회하고,
     * true/false이면 해당 상태의 센서만 조회함
     *
     * @param active 활성 상태 필터 (null: 전체, true: 활성, false: 비활성)
     * @return WiFi 센서 목록
     */
    @Override
    public List<WifiSensorResponseDto> getAllWifiSensors(Boolean active) {
        List<WifiSensor> sensors;

        // active 파라미터가 null이면 전체 센서를 조회함
        if (active == null) {
            sensors = wifiSensorRepository.findAll();
        } else {
            // active 값에 따라 필터링하여 조회함
            sensors = wifiSensorRepository.findByIsActive(active);
        }

        // WifiSensor 엔티티를 WifiSensorResponseDto로 변환하여 반환함
        return sensors.stream()
                .map(WifiSensorResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WifiSensorResponseDto updateWifiSensor(Long id, WifiSensorRequestDto requestDto) {
        WifiSensor wifiSensor = wifiSensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WifiSensor not found with id: " + id));

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        // sensorCode는 수정하지 않음 (자동 생성된 값 유지)
        wifiSensor.setLocation(location);
        wifiSensor.setStatus(requestDto.getStatus());
        wifiSensor.setSignalStrength(requestDto.getSignalStrength());
        wifiSensor.setDetectionRadius(requestDto.getDetectionRadius());

        // 'isActive' 변경시 마지막 변경 시각 업데이트
        if (requestDto.getIsActive() && !wifiSensor.getIsActive()) {
            wifiSensor.setLastActiveAt(LocalDateTime.now());
        }
        wifiSensor.setIsActive(requestDto.getIsActive());

        return WifiSensorResponseDto.from(wifiSensor);
    }

    @Override
    @Transactional
    public void deleteWifiSensor(Long id) {
        if (!wifiSensorRepository.existsById(id)) {
            throw new IllegalArgumentException("WifiSensor not found with id: " + id);
        }
        wifiSensorRepository.deleteById(id);
    }

}