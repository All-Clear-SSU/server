package opensource.project.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.CCTV;
import opensource.project.domain.Location;
import opensource.project.domain.WifiSensor;
import opensource.project.domain.enums.CCTVStatus;
import opensource.project.domain.enums.SensorStatus;
import opensource.project.dto.BuildingRegisterRequest;
import opensource.project.repository.CCTVRepository;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.WifiSensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 건물/장비 초기 등록 서비스.
 * 요구사항: CCTV 위치는 location_id 1부터, WiFi(CSI)는 location_id 101부터 할당.
 * CCTV id는 1부터, WiFi id도 1부터 순서대로 매핑.
 * location fullAddress만 입력받아 저장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BuildingRegistrationService {

    private final LocationRepository locationRepository;
    private final CCTVRepository cctvRepository;
    private final WifiSensorRepository wifiSensorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public void registerBuilding(BuildingRegisterRequest request) {
        List<BuildingRegisterRequest.CctvInput> cctvs =
                request.getCctvs() != null ? request.getCctvs() : Collections.emptyList();
        List<BuildingRegisterRequest.WifiInput> wifiSensors =
                request.getWifiSensors() != null ? request.getWifiSensors() : Collections.emptyList();

        // 1) 기존 데이터 초기화 (초기화 허용 전제)
        wifiSensorRepository.deleteAllInBatch();
        cctvRepository.deleteAllInBatch();
        locationRepository.deleteAllInBatch();

        // 2) 시퀀스 리셋 (H2/PostgreSQL 기준 RESTART 사용)
        resetSequence("LOCATION_SEQ", 1);
        resetSequence("CCTV_SEQ", 1);
        resetSequence("WIFI_SENSOR_SEQ", 1);

        // 3) CCTV용 Location 생성 (1부터)
        List<Location> cctvLocations = cctvs.stream()
                .map(input -> Location.builder()
                        .buildingName(request.getBuildingName())
                        .floor(null)
                        .roomNumber(null)
                        .fullAddress(input.getFullAddress())
                        .build())
                .collect(Collectors.toList());
        locationRepository.saveAll(cctvLocations);

        // 4) CCTV 등록 (id 1부터, 상태 WAIT/LIVE 선택: 기본 LIVE, isActive true)
        List<CCTV> savedCctvs = cctvs.stream()
                .sorted(Comparator.comparingInt(cctvs::indexOf))
                .map((input) -> {
                    int idx = cctvs.indexOf(input);
                    Location loc = cctvLocations.get(idx);
                    return CCTV.builder()
                            .cameraNumber(idx + 1)
                            .cctvCode("CCTV-" + (idx + 1))
                            .cctvName("CCTV-" + (idx + 1))
                            .rtspUrl(input.getRtspUrl())
                            .status(CCTVStatus.LIVE)
                            .location(loc)
                            .isActive(true)
                            .lastActiveAt(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
        cctvRepository.saveAll(savedCctvs);

        // 5) WiFi용 Location 시퀀스를 101로 재시작
        resetSequence("LOCATION_SEQ", 101);

        List<Location> wifiLocations = wifiSensors.stream()
                .map(input -> Location.builder()
                        .buildingName(request.getBuildingName())
                        .floor(null)
                        .roomNumber(null)
                        .fullAddress(input.getFullAddress())
                        .build())
                .collect(Collectors.toList());
        locationRepository.saveAll(wifiLocations);

        // 6) WiFi 센서 등록 (id 1부터, csiTopic 저장)
        List<WifiSensor> savedWifiSensors = wifiSensors.stream()
                .sorted(Comparator.comparingInt(wifiSensors::indexOf))
                .map(input -> {
                    int idx = wifiSensors.indexOf(input);
                    Location loc = wifiLocations.get(idx);
                    return WifiSensor.builder()
                            .sensorCode("WIFI-SENSOR-" + (idx + 1))
                            .location(loc)
                            .csiTopic(input.getCsiTopic())
                            .status(SensorStatus.ACTIVE)
                            .signalStrength(null)
                            .detectionRadius(null)
                            .isActive(true)
                            .lastActiveAt(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
        wifiSensorRepository.saveAll(savedWifiSensors);
    }

    @Transactional(readOnly = true)
    public List<String> getBuildingNames() {
        return locationRepository.findAll().stream()
                .map(Location::getBuildingName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void resetSequence(String sequenceName, long startWith) {
        try {
            // Oracle 12cR2+ 문법: RESTART START WITH
            entityManager.createNativeQuery("ALTER SEQUENCE " + sequenceName + " RESTART START WITH " + startWith)
                .executeUpdate();
        } catch (Exception e) {
            log.warn("Failed to reset sequence {}: {}", sequenceName, e.getMessage());
        }
    }
}
