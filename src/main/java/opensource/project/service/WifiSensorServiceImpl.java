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
        // WiFi 센서 id가 이미 존재하는지 확인
        if (wifiSensorRepository.findBySensorCode(requestDto.getSensorCode()).isPresent()) {
            throw new IllegalArgumentException("Sensor code already exists: " + requestDto.getSensorCode());
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        WifiSensor wifiSensor = WifiSensor.builder()
                .sensorCode(requestDto.getSensorCode())
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

    @Override
    public List<WifiSensorResponseDto> getAllWifiSensors() {
        return wifiSensorRepository.findAll().stream()
                .map(WifiSensorResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WifiSensorResponseDto updateWifiSensor(Long id, WifiSensorRequestDto requestDto) {
        WifiSensor wifiSensor = wifiSensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WifiSensor not found with id: " + id));

        // 중복된 Id인지 검사
        if (!wifiSensor.getSensorCode().equals(requestDto.getSensorCode())) {
            if (wifiSensorRepository.findBySensorCode(requestDto.getSensorCode()).isPresent()) {
                throw new IllegalArgumentException("Sensor code already exists: " + requestDto.getSensorCode());
            }
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        wifiSensor.setSensorCode(requestDto.getSensorCode());
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