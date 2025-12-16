package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Detection;
import opensource.project.domain.Location;
import opensource.project.domain.PriorityAssessment;
import opensource.project.domain.RecentSurvivorRecord;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DeleteReason;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.RecentSurvivorRecordResponseDto;
import opensource.project.dto.SurvivorRequestDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.PriorityAssessmentRepository;
import opensource.project.repository.RecentSurvivorRecordRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SurvivorServiceImpl implements SurvivorService {

    private final SurvivorRepository survivorRepository;
    private final LocationRepository locationRepository;
    private final PriorityAssessmentRepository priorityAssessmentRepository;
    private final WebSocketService webSocketService;
    private final DetectionRepository detectionRepository;
    private final RecentSurvivorRecordRepository recentSurvivorRecordRepository;

    // 새로운 생존자 정보 등록
    @Override
    @Transactional
    public SurvivorResponseDto createSurvivor(SurvivorRequestDto requestDto) {
        // 이미 존재하는 생존자인지 아닌지 확인
        if (survivorRepository.findBySurvivorNumber(requestDto.getSurvivorNumber()).isPresent()) {
            throw new IllegalArgumentException("Survivor number already exists: " + requestDto.getSurvivorNumber());
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        Survivor survivor = Survivor.builder()
                .survivorNumber(requestDto.getSurvivorNumber())
                .location(location)
                .currentStatus(requestDto.getCurrentStatus())
                .detectionMethod(requestDto.getDetectionMethod())
                .rescueStatus(requestDto.getRescueStatus())
                .firstDetectedAt(requestDto.getFirstDetectedAt() != null ? requestDto.getFirstDetectedAt() : LocalDateTime.now())
                .lastDetectedAt(requestDto.getLastDetectedAt() != null ? requestDto.getLastDetectedAt() : LocalDateTime.now())
                .isActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true)
                .isFalsePositive(requestDto.getIsFalsePositive() != null ? requestDto.getIsFalsePositive() : false)
                .falsePositiveReportedAt(requestDto.getFalsePositiveReportedAt())
                .build();

        Survivor savedSurvivor = survivorRepository.save(survivor);
        SurvivorResponseDto responseDto = SurvivorResponseDto.from(savedSurvivor);

        // WebSocket으로 새 생존자 추가 브로드캐스트
        webSocketService.broadcastNewSurvivorAdded(responseDto);

        return responseDto;
    }

    // 생존자 엔터티의 모든 인스턴스 반환 (활성 생존자만)
    @Override
    public List<SurvivorResponseDto> getAllSurvivors() {
        return survivorRepository.findByIsActiveTrue().stream()
                .map(SurvivorResponseDto::from)
                .collect(Collectors.toList());
    }

    // 생존자 엔터티의 특정 인스턴스 반환
    @Override
    public SurvivorResponseDto getSurvivor(Long id) {
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));
        return SurvivorResponseDto.from(survivor);
    }

    // 생존자 정보 수정
    @Override
    @Transactional
    public SurvivorResponseDto updateSurvivor(Long id, SurvivorRequestDto requestDto) {
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));

        // Check for duplicate survivor number if it's being changed
        if (!survivor.getSurvivorNumber().equals(requestDto.getSurvivorNumber())) {
            if (survivorRepository.findBySurvivorNumber(requestDto.getSurvivorNumber()).isPresent()) {
                throw new IllegalArgumentException("Survivor number already exists: " + requestDto.getSurvivorNumber());
            }
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        survivor.setSurvivorNumber(requestDto.getSurvivorNumber());
        survivor.setLocation(location);
        survivor.setCurrentStatus(requestDto.getCurrentStatus());
        survivor.setDetectionMethod(requestDto.getDetectionMethod());
        survivor.setRescueStatus(requestDto.getRescueStatus());

        if (requestDto.getFirstDetectedAt() != null) {
            survivor.setFirstDetectedAt(requestDto.getFirstDetectedAt());
        }
        if (requestDto.getLastDetectedAt() != null) {
            survivor.setLastDetectedAt(requestDto.getLastDetectedAt());
        }
        if (requestDto.getIsActive() != null) {
            survivor.setIsActive(requestDto.getIsActive());
        }
        if (requestDto.getIsFalsePositive() != null) {
            survivor.setIsFalsePositive(requestDto.getIsFalsePositive());
        }
        if (requestDto.getFalsePositiveReportedAt() != null) {
            survivor.setFalsePositiveReportedAt(requestDto.getFalsePositiveReportedAt());
        }

        SurvivorResponseDto responseDto = SurvivorResponseDto.from(survivor);

        // WebSocket으로 생존자 정보 업데이트 브로드캐스트
        webSocketService.broadcastSurvivorUpdate(id, responseDto);

        return responseDto;
    }

    @Override
    @Transactional
    // 생존자 목록에서 해당 id의 생존자를 제거
    // ✅ CASCADE 삭제: priority_assessment → detection → survivor 순서로 삭제
    public void deleteSurvivor(Long id, DeleteReason reason) {
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));

        // 타임아웃으로 삭제될 경우 마지막 상태 스냅샷을 보관
        if (reason == DeleteReason.TIMEOUT) {
            archiveSurvivorSnapshot(survivor);
        }

        // 1. PriorityAssessment 삭제 (Detection과의 외래키 제약 때문에 먼저 삭제)
        priorityAssessmentRepository.deleteBySurvivor_Id(id);

        // 2. Detection 삭제
        detectionRepository.deleteBySurvivorId(id);

        // 3. Survivor 삭제
        survivorRepository.deleteById(id);
    }

    // 구조 상태 변경하도록
    @Override
    @Transactional
    public SurvivorResponseDto updateRescueStatus(Long id, RescueStatus rescueStatus) {
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));

        survivor.setRescueStatus(rescueStatus);

        // Deactivate if rescued
        if (rescueStatus == RescueStatus.RESCUED) {
            survivor.setIsActive(false);
        }

        SurvivorResponseDto responseDto = SurvivorResponseDto.from(survivor);

        // WebSocket으로 구조 상태 변경 브로드캐스트
        webSocketService.broadcastSurvivorUpdate(id, responseDto);

        return responseDto;
    }

    // 탐지된 수단 찾기
    @Override
    public DetectionMethod getDetectionMethod(Long id) {
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));
        return survivor.getDetectionMethod();
    }

    @Override
    public PriorityScoreHistoryDto getLatestPriorityScore(Long id) {
        // 생존자가 존재하는지 확인
        Survivor survivor = survivorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + id));

        // 가장 최근 분석 점수 찾기
        return priorityAssessmentRepository.findFirstBySurvivor_IdOrderByAssessedAtDesc(id)
                .map(PriorityScoreHistoryDto::from)
                .orElseThrow(() -> new IllegalArgumentException("No priority assessment found for survivor with id: " + id));
    }

    /**
     * 타임아웃으로 삭제되기 직전의 생존자 정보를 스냅샷으로 보관한다.
     */
    private void archiveSurvivorSnapshot(Survivor survivor) {
        try {
            Detection latestDetection = detectionRepository.findBySurvivorIdOrderByDetectedAtDesc(survivor.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            PriorityAssessment latestAssessment = priorityAssessmentRepository
                    .findFirstBySurvivor_IdOrderByAssessedAtDesc(survivor.getId())
                    .orElse(null);

            Location location = survivor.getLocation();

            RecentSurvivorRecord record = RecentSurvivorRecord.builder()
                    .survivorId(survivor.getId())
                    .survivorNumber(survivor.getSurvivorNumber())
                    .buildingName(location != null ? location.getBuildingName() : null)
                    .floor(location != null ? location.getFloor() : null)
                    .roomNumber(location != null ? location.getRoomNumber() : null)
                    .fullAddress(location != null ? location.getFullAddress() : null)
                    .lastDetectedAt(latestDetection != null ? latestDetection.getDetectedAt() : survivor.getLastDetectedAt())
                    .lastPose(resolveLastPose(survivor, latestDetection))
                    .lastRiskScore(latestAssessment != null ? latestAssessment.getFinalRiskScore() : null)
                    .detectionMethod(survivor.getDetectionMethod())
                    .cctvId(latestDetection != null && latestDetection.getCctv() != null ? latestDetection.getCctv().getId() : null)
                    .wifiSensorId(latestDetection != null && latestDetection.getWifiSensor() != null ? latestDetection.getWifiSensor().getId() : null)
                    .aiAnalysisResult(latestDetection != null ? latestDetection.getAiAnalysisResult() : null)
                    .aiSummary(buildAiSummary(latestDetection))
                    .build();

            RecentSurvivorRecord saved = recentSurvivorRecordRepository.save(record);
            webSocketService.broadcastRecentRecordAdded(RecentSurvivorRecordResponseDto.from(saved));
        } catch (Exception e) {
            // 스냅샷 실패가 삭제 자체를 막지 않도록 한다.
            log.warn("Failed to archive survivor snapshot for survivor {}", survivor.getId(), e);
        }
    }

    private CurrentStatus resolveLastPose(Survivor survivor, Detection latestDetection) {
        if (latestDetection != null && latestDetection.getDetectedStatus() != null) {
            return latestDetection.getDetectedStatus();
        }
        return survivor.getCurrentStatus();
    }

    /**
     * pose/fire/smoke/confidence 기반 간단 상황 요약 생성
     */
    private String buildAiSummary(Detection latestDetection) {
        if (latestDetection == null) {
            return null;
        }

        CurrentStatus pose = latestDetection.getDetectedStatus();
        Integer fireCount = latestDetection.getFireCount();
        Integer smokeCount = latestDetection.getSmokeCount();
        Double confidence = latestDetection.getConfidence();

        boolean hasFire = fireCount != null && fireCount > 0;
        boolean hasSmoke = smokeCount != null && smokeCount > 0;

        StringBuilder sb = new StringBuilder();
        if (pose != null) {
            sb.append("생존자 감지");
            String poseText = switch (pose) {
                case FALLING -> "쓰러짐";
                case CRAWLING -> "기어감";
                case SITTING -> "앉아 있음";
                case STANDING -> "서 있음";
                default -> pose.name();
            };
            sb.append(" (자세: ").append(poseText).append(")");
        } else {
            sb.append("생존자 감지");
        }

        if (hasFire && hasSmoke) {
            sb.append(", 주변 화염 및 연기 감지");
        } else if (hasFire) {
            sb.append(", 주변 화염 감지");
        } else if (hasSmoke) {
            sb.append(", 주변 연기 감지");
        }

        if (confidence != null) {
            sb.append(" (신뢰도: ").append(String.format("%.2f", confidence)).append(")");
        }

        return sb.toString();
    }
}
