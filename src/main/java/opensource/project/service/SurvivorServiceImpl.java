package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorRequestDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.PriorityAssessmentRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurvivorServiceImpl implements SurvivorService {

    private final SurvivorRepository survivorRepository;
    private final LocationRepository locationRepository;
    private final PriorityAssessmentRepository priorityAssessmentRepository;
    private final WebSocketService webSocketService;

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
    public void deleteSurvivor(Long id) {
        if (!survivorRepository.existsById(id)) {
            throw new IllegalArgumentException("Survivor not found with id: " + id);
        }
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
}