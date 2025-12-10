package opensource.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.*;
import opensource.project.domain.enums.DetectionType;
import opensource.project.dto.*;
import opensource.project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectionServiceImpl implements DetectionService {

    private final DetectionRepository detectionRepository;
    private final SurvivorRepository survivorRepository;
    private final CCTVRepository cctvRepository;
    private final WifiSensorRepository wifiSensorRepository;
    private final LocationRepository locationRepository;
    private final WebSocketService webSocketService;
    private final PriorityAssessmentRepository priorityAssessmentRepository;
    private final PriorityService priorityService;
    private final ObjectDetectionApiClient objectDetectionApiClient;
    private final AIDetectionProcessorService aiDetectionProcessorService;

    @Override
    @Transactional
    public DetectionResponseDto createDetection(DetectionRequestDto requestDto) {
        Survivor survivor = survivorRepository.findById(requestDto.getSurvivorId())
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + requestDto.getSurvivorId()));

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        CCTV cctv = null;
        WifiSensor wifiSensor = null;

        // 적절한 탐지 수단인지 검증 및 그에 대응되는 탐지수단 id값이 입력되었는지 확인
        if (requestDto.getDetectionType() == DetectionType.CCTV) {
            if (requestDto.getCctvId() == null) {
                throw new IllegalArgumentException("CCTV ID is required for CCTV detection type");
            }
            cctv = cctvRepository.findById(requestDto.getCctvId())
                    .orElseThrow(() -> new IllegalArgumentException("CCTV not found with id: " + requestDto.getCctvId()));
        } else if (requestDto.getDetectionType() == DetectionType.WIFI) {
            if (requestDto.getWifiSensorId() == null) {
                throw new IllegalArgumentException("WiFi Sensor ID is required for WIFI detection type");
            }
            wifiSensor = wifiSensorRepository.findById(requestDto.getWifiSensorId())
                    .orElseThrow(() -> new IllegalArgumentException("WiFi Sensor not found with id: " + requestDto.getWifiSensorId()));
        }

        Detection detection = Detection.builder()
                .survivor(survivor)
                .detectionType(requestDto.getDetectionType())
                .cctv(cctv)
                .wifiSensor(wifiSensor)
                .location(location)
                .detectedAt(requestDto.getDetectedAt())
                .detectedStatus(requestDto.getDetectedStatus())
                .aiAnalysisResult(requestDto.getAiAnalysisResult())
                .aiModelVersion(requestDto.getAiModelVersion())
                .confidence(requestDto.getConfidence())
                .imageUrl(requestDto.getImageUrl())
                .videoUrl(requestDto.getVideoUrl())
                .signalStrength(requestDto.getSignalStrength())
                .rawData(requestDto.getRawData())
                .build();

        // DB 저장
        Detection savedDetection = detectionRepository.save(detection);

        // 생존자의 최근 탐지 시간과 위치를 업데이트
        survivor.setLastDetectedAt(requestDto.getDetectedAt());
        survivor.setLocation(location);
        if (requestDto.getDetectedStatus() != null) {
            survivor.setCurrentStatus(requestDto.getDetectedStatus());
        }

        // WebSocket으로 실시간 브로드캐스트
        DetectionResponseDto responseDto = DetectionResponseDto.from(savedDetection);
        webSocketService.broadcastDetectionUpdate(requestDto.getSurvivorId(), responseDto);

        return responseDto;
    }

    @Override
    public List<DetectionResponseDto> getAllDetections() {
        return detectionRepository.findAll().stream()
                .map(DetectionResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 생존자의 가장 최신 Detection 조회
    @Override
    public DetectionResponseDto getLatestDetectionBySurvivor(Long survivorId) {
        return getLatestDetectionBySurvivor(survivorId, null);
    }

    // 특정 생존자의 가장 최신 Detection 조회 (format 지원)
    public DetectionResponseDto getLatestDetectionBySurvivor(Long survivorId, String format) {
        // 생존자 확인
        if (!survivorRepository.existsById(survivorId)) {
            throw new IllegalArgumentException("Survivor not found with id: " + survivorId);
        }

        List<Detection> detections = detectionRepository.findBySurvivorIdOrderByDetectedAtDesc(survivorId);
        if (detections.isEmpty()) {
            throw new IllegalArgumentException("No detection found for survivor with id: " + survivorId);
        }

        Detection latestDetection = detections.get(0);
        DetectionResponseDto response = DetectionResponseDto.from(latestDetection);

        // format=summary인 경우 aiAnalysisResult를 한글 요약으로 변환
        if ("summary".equals(format)) {
            String situationSummary = convertToSituationSummary(
                    latestDetection.getAiAnalysisResult(),
                    latestDetection
            );
            response.setAiAnalysisResult(situationSummary);
        }

        return response;
    }


    @Override
    @Transactional
    public DetectionResponseDto updateDetection(Long id, DetectionRequestDto requestDto) {
        Detection detection = detectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Detection not found with id: " + id));

        Survivor survivor = survivorRepository.findById(requestDto.getSurvivorId())
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + requestDto.getSurvivorId()));

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        CCTV cctv = null;
        WifiSensor wifiSensor = null;

        if (requestDto.getDetectionType() == DetectionType.CCTV) {
            if (requestDto.getCctvId() == null) {
                throw new IllegalArgumentException("CCTV ID is required for CCTV detection type");
            }
            cctv = cctvRepository.findById(requestDto.getCctvId())
                    .orElseThrow(() -> new IllegalArgumentException("CCTV not found with id: " + requestDto.getCctvId()));
        } else if (requestDto.getDetectionType() == DetectionType.WIFI) {
            if (requestDto.getWifiSensorId() == null) {
                throw new IllegalArgumentException("WiFi Sensor ID is required for WIFI detection type");
            }
            wifiSensor = wifiSensorRepository.findById(requestDto.getWifiSensorId())
                    .orElseThrow(() -> new IllegalArgumentException("WiFi Sensor not found with id: " + requestDto.getWifiSensorId()));
        }

        detection.setSurvivor(survivor);
        detection.setDetectionType(requestDto.getDetectionType());
        detection.setCctv(cctv);
        detection.setWifiSensor(wifiSensor);
        detection.setLocation(location);
        detection.setDetectedAt(requestDto.getDetectedAt());
        detection.setDetectedStatus(requestDto.getDetectedStatus());
        detection.setAiAnalysisResult(requestDto.getAiAnalysisResult());
        detection.setAiModelVersion(requestDto.getAiModelVersion());
        detection.setConfidence(requestDto.getConfidence());
        detection.setImageUrl(requestDto.getImageUrl());
        detection.setVideoUrl(requestDto.getVideoUrl());
        detection.setSignalStrength(requestDto.getSignalStrength());
        detection.setRawData(requestDto.getRawData());

        // WebSocket으로 실시간 브로드캐스트
        DetectionResponseDto responseDto = DetectionResponseDto.from(detection);
        webSocketService.broadcastDetectionUpdate(requestDto.getSurvivorId(), responseDto);

        return responseDto;
    }

    @Override
    @Transactional
    public void deleteDetection(Long id) {
        if (!detectionRepository.existsById(id)) {
            throw new IllegalArgumentException("Detection not found with id: " + id);
        }
        detectionRepository.deleteById(id);
    }

    // 특정 생존자의 종합 분석 정보 조회(Survivor, Detection, PriorityAssessment를 통합하여 반환)
    @Override
    public SurvivorAnalysisDto getSurvivorAnalysis(Long survivorId) {
        return getSurvivorAnalysis(survivorId, null);
    }

    // 특정 생존자의 종합 분석 정보 조회 (format 지원)
    public SurvivorAnalysisDto getSurvivorAnalysis(Long survivorId, String format) {
        // 생존자 조회
        Survivor survivor = survivorRepository.findById(survivorId)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + survivorId));

        // 최신 Detection 조회
        List<Detection> detections = detectionRepository.findBySurvivorIdOrderByDetectedAtDesc(survivorId);
        Detection latestDetection = detections.isEmpty() ? null : detections.get(0);

        // 최신 PriorityAssessment 조회
        PriorityAssessment assessment = priorityAssessmentRepository
                .findFirstBySurvivor_IdOrderByAssessedAtDesc(survivorId)
                .orElse(null);

        SurvivorAnalysisDto analysisDto = SurvivorAnalysisDto.from(survivor, latestDetection, assessment);

        // format=summary인 경우 aiAnalysisResult를 한글 요약으로 변환
        if ("summary".equals(format) && latestDetection != null && analysisDto.getAiAnalysisResult() != null) {
            String situationSummary = convertToSituationSummary(
                    latestDetection.getAiAnalysisResult(),
                    latestDetection
            );
            analysisDto.setAiAnalysisResult(situationSummary);
        }

        return analysisDto;
    }

    // Object_Detection 모델을 호출 -> Object Detection 수행 -> Detection과 PriorityAssessment에 저장(더미)하고 결과 반환
    @Override
    @Transactional
    public ImageAnalysisResponseDto analyzeImage(
            Long survivorId,
            Long locationId,
            Long cctvId,
            MultipartFile imageFile
    ) throws IOException {
        // 0. 생존자, 위치, CCTV 엔터티 조회
        Survivor survivor = survivorRepository.findById(survivorId)
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + survivorId));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + locationId));

        CCTV cctv = cctvRepository.findById(cctvId)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found with id: " + cctvId));

        // 1. 객체 탐지 모델 API 호출하여 Object Detection 결과 받기
        ObjectDetectionResultDto detectionResult = objectDetectionApiClient.detectObjects(imageFile);

        // 2. 객체 탐지 모델 API 호출하여 분석 이미지 받기
        byte[] analyzedImageBytes = objectDetectionApiClient.getAnalyzedImage(imageFile);
        String analyzedImageBase64 = Base64.getEncoder().encodeToString(analyzedImageBytes);

        // 3. 생존자 상태메시지 생성 (더미)
        String statusMessage = aiDetectionProcessorService.generateSurvivorStatusMessage(detectionResult);

        // 4. Detection 엔터티 생성 및 저장
        ObjectDetectionSummaryDto summary =
                detectionResult.getSummary();
        Detection detection = Detection.builder()
                .survivor(survivor)
                .detectionType(DetectionType.CCTV)
                .cctv(cctv)
                .location(location)
                .detectedAt(LocalDateTime.now())
                .aiAnalysisResult(statusMessage)
                .aiModelVersion("ObjectDetection-v1.0")
                .fireCount(summary.getFireCount() != null ? summary.getFireCount() : 0)
                .humanCount(summary.getHumanCount() != null ? summary.getHumanCount() : 0)
                .smokeCount(summary.getSmokeCount() != null ? summary.getSmokeCount() : 0)
                .totalObjects(summary.getTotalObjects() != null ? summary.getTotalObjects() : 0)
                .analyzedImage(analyzedImageBytes)
                .build();

        Detection savedDetection = detectionRepository.save(detection);

        // 5. PriorityService를 통해 더미 데이터로 PriorityAssessment 생성
        PriorityAssessmentRequestDto priorityRequest =
                priorityService.createDummyPriorityAssessment(
                savedDetection.getId(),
                survivorId);
        PriorityAssessmentResponseDto priorityResponse =
                priorityService.createPriorityAssessment(priorityRequest);

        // 6. 최종 응답 DTO 생성 및 반환
        return ImageAnalysisResponseDto.builder()
                .fireCount(savedDetection.getFireCount())
                .humanCount(savedDetection.getHumanCount())
                .smokeCount(savedDetection.getSmokeCount())
                .totalObjects(savedDetection.getTotalObjects())
                .survivorStatusMessage(savedDetection.getAiAnalysisResult())
                .statusScore(priorityResponse.getStatusScore())
                .environmentScore(priorityResponse.getEnvironmentScore())
                .confidenceCoefficient(priorityResponse.getConfidenceCoefficient())
                .finalRiskScore(priorityResponse.getFinalRiskScore())
                .detectionId(savedDetection.getId())
                .priorityAssessmentId(priorityResponse.getId())
                .analyzedImageBase64(analyzedImageBase64)
                .build();
    }

    // Detection ID로 분석 이미지 조회
    @Override
    public byte[] getAnalyzedImage(Long detectionId) {
        Detection detection = detectionRepository.findById(detectionId)
                .orElseThrow(() -> new IllegalArgumentException("Detection not found with id: " + detectionId));

        byte[] analyzedImage = detection.getAnalyzedImage();
        if (analyzedImage == null) {
            throw new IllegalArgumentException("No analyzed image found for detection id: " + detectionId);
        }

        return analyzedImage;
    }

    /**
     * AI 모델의 분석 결과를 받아 Survivor, Detection, PriorityAssessment 생성
     * AIDetectionProcessorService로 위임
     */
    @Override
    @Transactional
    public void processAIDetectionResult(AIDetectionResultDto aiResult,
                                          Long cctvId,
                                          Long locationId,
                                          String videoUrl) {
        // AI 탐지 결과 처리를 전담 서비스로 위임
        aiDetectionProcessorService.processAIDetectionResult(aiResult, cctvId, locationId, videoUrl);
    }

    /**
     * AI 분석 결과 JSON을 한글 상황 요약으로 변환
     * DB에 저장된 JSON은 그대로 유지하고, API 응답용으로만 변환
     */
    public String convertToSituationSummary(String aiAnalysisJson, Detection detection) {
        // JSON이 아니거나 null인 경우 원본 그대로 반환
        if (aiAnalysisJson == null || !aiAnalysisJson.trim().startsWith("{")) {
            return aiAnalysisJson;
        }

        try {
            // WiFi Detection인 경우 다르게 처리
            if (detection.getDetectionType() == DetectionType.WIFI) {
                return generateWifiSituationMessage(aiAnalysisJson, detection);
            }

            // CCTV Detection 처리 (기존 로직)
            ObjectMapper mapper = new ObjectMapper();
            AIDetectionResultDto.DetectionObject detectionObject =
                    mapper.readValue(aiAnalysisJson, AIDetectionResultDto.DetectionObject.class);

            String pose = detectionObject.getPose();

            // Detection 엔티티에서 fire/smoke count 가져오기
            Integer fireCount = detection.getFireCount() != null ? detection.getFireCount() : 0;
            Integer smokeCount = detection.getSmokeCount() != null ? detection.getSmokeCount() : 0;

            return generateSituationMessage(pose, fireCount, smokeCount);

        } catch (Exception e) {
            log.warn("Failed to parse AI analysis result to situation summary", e);
            return aiAnalysisJson; // 파싱 실패 시 원본 반환
        }
    }

    /**
     * WiFi Detection용 상황 요약 메시지 생성
     */
    private String generateWifiSituationMessage(String aiAnalysisJson, Detection detection) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            WifiAnalysisDataDto wifiAnalysis = mapper.readValue(aiAnalysisJson, WifiAnalysisDataDto.class);

            StringBuilder message = new StringBuilder("WiFi 센서 탐지");

            // 움직임 감지
            if (Boolean.TRUE.equals(wifiAnalysis.getMovementDetected())) {
                message.append(" - 움직임 감지");
                if (wifiAnalysis.getMovementIntensity() != null) {
                    double intensity = wifiAnalysis.getMovementIntensity();
                    if (intensity > 0.7) {
                        message.append(" (강함)");
                    } else if (intensity > 0.3) {
                        message.append(" (보통)");
                    } else {
                        message.append(" (약함)");
                    }
                }
            }

            // 호흡 감지
            if (Boolean.TRUE.equals(wifiAnalysis.getBreathingDetected())) {
                message.append(" - 호흡 감지");
                if (wifiAnalysis.getBreathingRate() != null) {
                    message.append(String.format(" (%.1f BPM)", wifiAnalysis.getBreathingRate()));
                }
            }

            // 신호 강도
            if (detection.getSignalStrength() != null) {
                message.append(String.format(" - 신호 강도: %d dBm", detection.getSignalStrength()));
            }

            return message.toString();
        } catch (Exception e) {
            log.warn("Failed to parse WiFi analysis data", e);
            return "WiFi 센서 탐지";
        }
    }

    /**
     * pose, fire_count, smoke_count를 조합하여 한글 상황 설명 생성
     */
    private String generateSituationMessage(String pose, int fireCount, int smokeCount) {
        StringBuilder message = new StringBuilder();

        // 1. 주변 환경 상황
        if (fireCount > 0 && smokeCount > 0) {
            message.append("주변 화염 및 연기 감지");
        } else if (fireCount > 0) {
            message.append("주변 화염 감지");
        } else if (smokeCount > 0) {
            message.append("주변 연기 감지");
        }

        // 구분자 추가
        if (message.length() > 0) {
            message.append(", ");
        }

        // 2. 생존자 자세 상태
        if (pose == null) {
            message.append("생존자 발견");
        } else {
            switch (pose.toLowerCase()) {
                case "falling", "fallen" -> message.append("생존자 쓰러져 있음");
                case "crawling" -> message.append("생존자 기어가는 중");
                case "sitting" -> message.append("생존자 앉아 있음");
                case "standing" -> message.append("생존자 서 있음");
                default -> message.append("생존자 발견");
            }
        }

        return message.toString();
    }

}