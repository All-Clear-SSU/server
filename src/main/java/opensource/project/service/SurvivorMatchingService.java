package opensource.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.CCTV;
import opensource.project.domain.Detection;
import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.AIDetectionResultDto;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 생존자 매칭 서비스
 * AI 탐지 결과를 기반으로 기존 생존자를 찾거나 새로운 생존자를 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurvivorMatchingService {

    private final SurvivorRepository survivorRepository;
    private final DetectionRepository detectionRepository;

    // 바운딩 박스 중심점 거리 기반 매칭 임계값 (픽셀)
    // CCTV 회전 시에도 안정적으로 추적할 수 있도록 임계값 증가
    private static final double DISTANCE_THRESHOLD = 300.0;

    /**
     * 기존 생존자를 찾거나 새로 생성
     * 바운딩박스 중심점 거리 기반으로 가장 가까운 생존자 매칭 (이미 매칭된 생존자 제외)
     * 같은 CCTV에서 분석된 영상만 기존 생존자와 매칭, 다른 CCTV는 무조건 새 생존자 생성
     */
    public Survivor findOrCreateSurvivor(AIDetectionResultDto.DetectionObject humanDetection,
                                          Location location,
                                          CCTV cctv,
                                          LocalDateTime now,
                                          Set<Long> matchedSurvivorIds) {

        // 동일 위치의 활성 생존자 목록 조회
        List<Survivor> activeSurvivors = survivorRepository.findActiveByLocation(location.getId());

        if (activeSurvivors.isEmpty()) {
            // 첫 생존자인 경우 새로 생성
            return createSurvivorFromAI(humanDetection, location, now);
        }

        // 바운딩 박스 중심점 계산
        AIDetectionResultDto.BoundingBox box = humanDetection.getBox();
        double centerX = (box.getX1() + box.getX2()) / 2.0;
        double centerY = (box.getY1() + box.getY2()) / 2.0;

        // 가장 가까운 생존자 찾기
        Survivor closestSurvivor = null;
        double minDistance = DISTANCE_THRESHOLD;

        for (Survivor survivor : activeSurvivors) {
            // 이미 이번 프레임에서 매칭된 생존자는 건너뜀 (중복 매칭 방지)
            if (matchedSurvivorIds.contains(survivor.getId())) {
                continue;
            }

            // 같은 CCTV에서의 가장 최근 Detection만 조회 (다른 CCTV의 Detection은 무시)
            List<Detection> recentDetections = detectionRepository
                    .findBySurvivorIdAndCctvIdOrderByDetectedAtDesc(survivor.getId(), cctv.getId());

            if (recentDetections.isEmpty()) {
                // 이 생존자는 해당 CCTV에서 탐지된 적이 없음 → 매칭 대상 아님
                continue;
            }

            Detection lastDetection = recentDetections.get(0);

            // JSON에서 바운딩박스 추출
            // aiAnalysisResult가 JSON 형식인 경우에만 파싱 시도
            String aiAnalysisJson = lastDetection.getAiAnalysisResult();
            if (aiAnalysisJson == null || !aiAnalysisJson.trim().startsWith("{")) {
                // JSON이 아닌 경우 (예: "감지된 객체: ...") 건너뜀
                continue;
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                AIDetectionResultDto.DetectionObject lastObject =
                        mapper.readValue(aiAnalysisJson,
                                AIDetectionResultDto.DetectionObject.class);

                AIDetectionResultDto.BoundingBox lastBox = lastObject.getBox();
                double lastCenterX = (lastBox.getX1() + lastBox.getX2()) / 2.0;
                double lastCenterY = (lastBox.getY1() + lastBox.getY2()) / 2.0;

                // 유클리드 거리 계산
                double distance = Math.sqrt(
                        Math.pow(centerX - lastCenterX, 2) +
                        Math.pow(centerY - lastCenterY, 2)
                );

                // 임계값 이내이고 현재까지 찾은 것보다 더 가까우면 업데이트
                if (distance < minDistance) {
                    minDistance = distance;
                    closestSurvivor = survivor;
                }

            } catch (Exception e) {
                log.warn("Failed to parse bounding box from detection for survivor {}",
                        survivor.getId(), e);
            }
        }

        // 가장 가까운 생존자가 있으면 반환
        if (closestSurvivor != null) {
            log.info("Found matching survivor #{} (distance: {}px)",
                    closestSurvivor.getSurvivorNumber(), String.format("%.2f", minDistance));
            return closestSurvivor;
        }

        // 매칭되는 생존자가 없으면 새로 생성
        return createSurvivorFromAI(humanDetection, location, now);
    }

    /**
     * AI 분석 결과로부터 Survivor 엔티티 생성
     */
    @Transactional
    public Survivor createSurvivorFromAI(AIDetectionResultDto.DetectionObject humanDetection,
                                          Location location,
                                          LocalDateTime now) {

        // Pose에 따른 CurrentStatus 매핑
        CurrentStatus status = mapPoseToStatus(humanDetection.getPose());

        Survivor survivor = Survivor.builder()
                .survivorNumber(generateSurvivorNumber())
                .location(location)
                .currentStatus(status)
                .detectionMethod(DetectionMethod.CCTV) // CCTV로 촬영된 영상을 AI가 분석
                .rescueStatus(RescueStatus.WAITING)
                .firstDetectedAt(now)
                .lastDetectedAt(now)
                .isActive(true)
                .isFalsePositive(false)
                .build();

        // DB에 저장
        return survivorRepository.save(survivor);
    }

    /**
     * Pose를 CurrentStatus로 매핑
     * AI 모델 클래스: ["Crawling", "Falling", "Sitting", "Standing"]
     */
    public CurrentStatus mapPoseToStatus(String pose) {
        if (pose == null) {
            return CurrentStatus.STANDING; // 기본값
        }

        return switch (pose.toLowerCase()) {
            case "falling", "fall", "fallen", "lying" -> CurrentStatus.FALLING;
            case "crawling" -> CurrentStatus.CRAWLING;
            case "sitting" -> CurrentStatus.SITTING;
            case "standing" -> CurrentStatus.STANDING;
            default -> CurrentStatus.STANDING;
        };
    }

    /**
     * 생존자 번호 생성 (자동 증가)
     */
    private int generateSurvivorNumber() {
        Integer maxNumber = survivorRepository.findMaxSurvivorNumber();
        return maxNumber != null ? maxNumber + 1 : 1;
    }
}