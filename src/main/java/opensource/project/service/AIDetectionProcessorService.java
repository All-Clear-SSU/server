package opensource.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.CCTV;
import opensource.project.domain.Detection;
import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.DetectionType;
import opensource.project.dto.*;
import opensource.project.repository.CCTVRepository;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 탐지 결과 처리 서비스
 * AI 모델의 분석 결과를 받아 Detection, Survivor, PriorityAssessment 생성 및 업데이트를 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIDetectionProcessorService {

    private final DetectionRepository detectionRepository;
    private final SurvivorRepository survivorRepository;
    private final CCTVRepository cctvRepository;
    private final LocationRepository locationRepository;
    private final WebSocketService webSocketService;
    private final PriorityService priorityService;
    private final SurvivorMatchingService survivorMatchingService;

    /**
     * AI 모델의 분석 결과를 받아 Survivor, Detection, PriorityAssessment 생성
     * 전달 받는 정보:
     * - aiResult: AI 모델 분석 결과
     * - cctvId: CCTV ID
     * - locationId: 위치 ID (같은 영상의 모든 생존자는 같은 위치)
     * - videoUrl: 영상 URL
     */
    @Transactional
    public void processAIDetectionResult(AIDetectionResultDto aiResult,
                                          Long cctvId,
                                          Long locationId,
                                          String videoUrl) {

        log.info("Processing AI detection result for CCTV {} at location {}", cctvId, locationId);

        // Null 체크 validation
        if (cctvId == null) {throw new IllegalArgumentException("cctvId must not be null");}
        if (locationId == null) {throw new IllegalArgumentException("locationId must not be null");}
        if (aiResult == null) {throw new IllegalArgumentException("aiResult must not be null");}

        // CCTV와 Location 조회
        CCTV cctv = cctvRepository.findById(cctvId)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + cctvId));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));

        // 모든 탐지 객체 처리
        List<AIDetectionResultDto.DetectionObject> allDetections = aiResult.getDetections();
        AIDetectionResultDto.DetectionSummary summary = aiResult.getSummary();

        // Null 체크
        if (summary == null) {
            log.warn("Summary is null in AI result. Using default values.");
            summary = new AIDetectionResultDto.DetectionSummary(0, 0, 0, 0);
        }

        if (allDetections == null || allDetections.isEmpty()) {
            log.warn("No detections found in AI result. Skipping processing.");
            return;
        }

        log.info("Processing {} detections (Fire: {}, Human: {}, Smoke: {})",
                allDetections.size(),
                summary.getFireCount(),
                summary.getHumanCount(),
                summary.getSmokeCount());

        // 현재 프레임에서 이미 매칭된 생존자 ID 추적 (중복 매칭 방지)
        Set<Long> matchedSurvivorIds = new HashSet<>();

        // 사람(Human/Person 등) 객체만 처리
        int humanProcessed = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if (isHumanDetection(detection)) {
                log.info("Processing human/person detection #{} - class: {}, pose: {}, confidence: {}",
                        ++humanProcessed, detection.getClassName(), detection.getPose(), detection.getConfidence());
                processHumanDetection(detection, allDetections, summary, cctv, location, videoUrl, matchedSurvivorIds);
            }
        }

        log.info("AI detection processing completed. Total detections: {}, Humans processed: {}, Matched survivors: {}",
                allDetections.size(), humanProcessed, matchedSurvivorIds.size());

    }

    /**
     * AI 모델이 사람을 "human" 외에 "person", "people" 등으로 표기할 때를 포함해 판단
     */
    private boolean isHumanDetection(AIDetectionResultDto.DetectionObject detection) {
        if (detection == null || detection.getClassName() == null) return false;
        String cls = detection.getClassName().trim().toLowerCase();
        return "human".equals(cls) || "person".equals(cls) || "people".equals(cls);
    }


     // 개별 Human 탐지 처리, 기존 생존자가 있으면 재사용, 없으면 새로 생성
    private void processHumanDetection(AIDetectionResultDto.DetectionObject humanDetection,
                                        List<AIDetectionResultDto.DetectionObject> allDetections,
                                        AIDetectionResultDto.DetectionSummary summary,
                                        CCTV cctv,
                                        Location location,
                                        String videoUrl,
                                        Set<Long> matchedSurvivorIds) {

        LocalDateTime now = LocalDateTime.now();

        // 1. 기존 생존자 찾기 (같은 CCTV의 바운딩박스 유사도 기반, 이미 매칭된 생존자 제외)
        Survivor survivor = survivorMatchingService.findOrCreateSurvivor(
                humanDetection, location, cctv, now, matchedSurvivorIds);
        boolean isNewSurvivor = survivor.getId() == null;

        if (!isNewSurvivor) {
            // 기존 생존자인 경우 정보 업데이트
            survivor.setCurrentStatus(survivorMatchingService.mapPoseToStatus(humanDetection.getPose()));
            survivor.setLastDetectedAt(now);
            survivorRepository.save(survivor);
            log.info("Updated existing survivor #{}", survivor.getSurvivorNumber());

            // WebSocket으로 기존 생존자 정보 업데이트 브로드캐스트
            webSocketService.broadcastSurvivorUpdate(survivor.getId(), SurvivorResponseDto.from(survivor));
        } else {
            // 새 생존자인 경우 저장
            survivor = survivorRepository.save(survivor);
            log.info("Created new survivor #{} with pose: {}", survivor.getSurvivorNumber(), humanDetection.getPose());

            // WebSocket으로 새 생존자 추가 브로드캐스트
            webSocketService.broadcastNewSurvivorAdded(SurvivorResponseDto.from(survivor));
        }

        // 현재 프레임에서 매칭된 생존자로 기록
        matchedSurvivorIds.add(survivor.getId());

        // 2. Detection 생성 (매 프레임마다 새로 생성 - 시계열 추적용)
        Detection detection = createDetectionFromAI(humanDetection, summary, survivor, cctv, location, videoUrl, now);

        // 3. PriorityAssessment 생성 (위험도 점수 계산 및 업데이트)
        priorityService.createAssessmentFromAI(humanDetection, allDetections, summary, survivor, detection);
    }

    // AI 분석 결과로부터 Detection 엔티티 생성
    private Detection createDetectionFromAI(AIDetectionResultDto.DetectionObject humanDetection,
                                             AIDetectionResultDto.DetectionSummary summary,
                                             Survivor survivor,
                                             CCTV cctv,
                                             Location location,
                                             String videoUrl,
                                             LocalDateTime now) {

        // AI 분석 결과를 JSON으로 저장
        String aiAnalysisJson;
        try {
            aiAnalysisJson = new ObjectMapper().writeValueAsString(humanDetection);
        } catch (Exception e) {
            log.error("Failed to convert AI result to JSON", e);
            aiAnalysisJson = "{}";
        }

        // 새로운 Detection 객체 생성
        Detection detection = Detection.builder()
                .survivor(survivor)
                .detectionType(DetectionType.CCTV) // AI 비전 분석
                .cctv(cctv)
                .location(location)
                .detectedAt(now)
                .detectedStatus(survivorMatchingService.mapPoseToStatus(humanDetection.getPose()))
                .aiAnalysisResult(aiAnalysisJson)
                .aiModelVersion("YOLO-ONNX-v1.0")
                .confidence(humanDetection.getConfidence())
                .videoUrl(videoUrl)
                .fireCount(summary.getFireCount())
                .humanCount(summary.getHumanCount())
                .smokeCount(summary.getSmokeCount())
                .totalObjects(summary.getTotalObjects())
                .build();

        // DB에 저장
        Detection savedDetection = detectionRepository.save(detection);

        // WebSocket으로 실시간 브로드캐스트
        DetectionResponseDto responseDto = DetectionResponseDto.from(savedDetection);
        webSocketService.broadcastDetectionUpdate(survivor.getId(), responseDto);

        return savedDetection;
    }


    // Object Detection 결과를 기반으로 생존자 상태메시지 생성(더미)
    public String generateSurvivorStatusMessage(ObjectDetectionResultDto detectionResult) {
        ObjectDetectionSummaryDto summary = detectionResult.getSummary();

        // null safe 처리
        int humanCount = summary.getHumanCount() != null ? summary.getHumanCount() : 0;
        int fireCount = summary.getFireCount() != null ? summary.getFireCount() : 0;
        int smokeCount = summary.getSmokeCount() != null ? summary.getSmokeCount() : 0;

        StringBuilder message = new StringBuilder();
        message.append("감지된 객체: ");

        if (humanCount > 0) {
            message.append("사람 ").append(humanCount).append("명");
        }
        if (fireCount > 0) {
            if (humanCount > 0) message.append(", ");
            message.append("화재 ").append(fireCount).append("건");
        }
        if (smokeCount > 0) {
            if (humanCount > 0 || fireCount > 0) message.append(", ");
            message.append("연기 ").append(smokeCount).append("건");
        }

        message.append(". ");

        // 상태 판단 (더미 로직)
        if (fireCount > 0 || smokeCount > 0) {
            message.append("위험 상황 감지됨");
        } else if (humanCount > 0) {
            message.append("생존자 발견");
        } else {
            message.append("정상 상태");
        }

        return message.toString();
    }
}
