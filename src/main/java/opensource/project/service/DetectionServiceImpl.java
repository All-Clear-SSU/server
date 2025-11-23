package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.DetectionType;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.*;
import opensource.project.repository.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
    private final RestTemplate restTemplate;

    private static final String Objectdetection_API_BASE_URL = "http://localhost:8000";
    private static final String PREDICT_ENDPOINT = "/predict";
    private static final String PREDICT_IMAGE_ENDPOINT = "/predict_image";
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;

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
        // 생존자 확인
        if (!survivorRepository.existsById(survivorId)) {
            throw new IllegalArgumentException("Survivor not found with id: " + survivorId);
        }

        List<Detection> detections = detectionRepository.findBySurvivorIdOrderByDetectedAtDesc(survivorId);
        if (detections.isEmpty()) {
            throw new IllegalArgumentException("No detection found for survivor with id: " + survivorId);
        }

        return DetectionResponseDto.from(detections.get(0));
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

        return SurvivorAnalysisDto.from(survivor, latestDetection, assessment);
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
        ObjectDetectionResultDto detectionResult =
                callObjectDetectionAPI(imageFile);

        // 2. 객체 탐지 모델 API 호출하여 분석 이미지 받기
        byte[] analyzedImageBytes =
                callAnalyzedImageAPI(imageFile);
        String analyzedImageBase64 =
                Base64.getEncoder().encodeToString(analyzedImageBytes);

        // 3. 생존자 상태메시지 생성 (더미)
        String statusMessage =
                generateSurvivorStatusMessage(detectionResult);

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

    // 객체 탐지 모델 API를 호출하여 Object Detection 결과를 받아옴
    private ObjectDetectionResultDto callObjectDetectionAPI(MultipartFile imageFile)
            throws IOException {
        String url = Objectdetection_API_BASE_URL + PREDICT_ENDPOINT +
                "?conf_threshold=" + DEFAULT_CONFIDENCE_THRESHOLD;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

       try {
            ResponseEntity<ObjectDetectionResultDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    ObjectDetectionResultDto.class
            );

            ObjectDetectionResultDto result = response.getBody();
            if (result == null || result.getSummary() == null) {
                throw new RuntimeException("Object Detection API 응답이 비어있습니다");
            }
            log.info("Object Detection 완료 - fire: {}, human: {}, smoke: {}, total: {}",
                    result.getSummary().getFireCount(),
                    result.getSummary().getHumanCount(),
                    result.getSummary().getSmokeCount(),
                    result.getSummary().getTotalObjects());

            return result;
        } catch (Exception e) {
            log.error("Object Detection API 호출 실패", e);
            throw new RuntimeException("Object Detection 모델 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // Object Detection 모델 API를 호출하여 분석된 이미지를 byte[]로 받아옴
    private byte[] callAnalyzedImageAPI(MultipartFile imageFile) throws IOException {
        String url = Objectdetection_API_BASE_URL + PREDICT_IMAGE_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            byte[] imageBytes = response.getBody();
            if (imageBytes != null) {
                log.info("분석 이미지 획득 성공 (크기: {} bytes)", imageBytes.length);
                return imageBytes;
            } else {
                throw new RuntimeException("분석 이미지를 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("분석 이미지 API 호출 실패", e);
            throw new RuntimeException("분석 이미지 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
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

    // Object Detection 결과를 기반으로 생존자 상태메시지 생성(더미)
    private String generateSurvivorStatusMessage(ObjectDetectionResultDto detectionResult) {
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

    /**
     * AI 모델의 분석 결과를 받아 Survivor, Detection, PriorityAssessment 생성
     * 전달 받는 정보:
     * aiResult: AI 모델 분석 결과
     * cctvId: CCTV ID
     * locationId: 위치 ID (같은 영상의 모든 생존자는 같은 위치)
     * videoUrl: 영상 URL
     */
    @Override
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
        if (allDetections == null || allDetections.isEmpty()) {
            log.warn("No detections found in AI result. Skipping processing.");
            return;
        }

        if (summary == null) {
            log.warn("Summary is null in AI result. Using default values.");
            summary = new AIDetectionResultDto.DetectionSummary(0, 0, 0, 0);
        }

        log.info("Processing {} detections (Fire: {}, Human: {}, Smoke: {})",
                allDetections.size(),
                summary.getFireCount(),
                summary.getHumanCount(),
                summary.getSmokeCount());

        // 현재 프레임에서 이미 매칭된 생존자 ID 추적 (중복 매칭 방지)
        Set<Long> matchedSurvivorIds = new HashSet<>();

        // Human 객체만 처리
        int humanProcessed = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("human".equalsIgnoreCase(detection.getClassName())) {
                log.info("Processing human detection #{} - pose: {}, confidence: {}",
                        ++humanProcessed, detection.getPose(), detection.getConfidence());
                processHumanDetection(detection, allDetections, summary, cctv, location, videoUrl, matchedSurvivorIds);
            }
        }

        log.info("AI detection processing completed. Total detections: {}, Humans processed: {}, Matched survivors: {}",
                allDetections.size(), humanProcessed, matchedSurvivorIds.size());
    }


    // 개별 Human 탐지 처리, 기존 생존자가 있으면 재사용, 없으면 새로 생성
    private void processHumanDetection(AIDetectionResultDto.DetectionObject humanDetection,
                                        List<AIDetectionResultDto.DetectionObject> allDetections,
                                        AIDetectionResultDto.DetectionSummary summary,
                                        CCTV cctv,
                                        Location location,
                                        String videoUrl,
                                        java.util.Set<Long> matchedSurvivorIds) {

        LocalDateTime now = LocalDateTime.now();

        // 1. 기존 생존자 찾기 (같은 CCTV의 바운딩박스 유사도 기반, 이미 매칭된 생존자 제외)
        Survivor survivor = findOrCreateSurvivor(humanDetection, location, cctv, now, matchedSurvivorIds);
        boolean isNewSurvivor = survivor.getId() == null;

        if (!isNewSurvivor) {
            // 기존 생존자인 경우 정보 업데이트
            survivor.setCurrentStatus(mapPoseToStatus(humanDetection.getPose()));
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

    // AI 분석 결과로부터 Survivor 엔티티 생성
    private Survivor createSurvivorFromAI(AIDetectionResultDto.DetectionObject humanDetection,
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

        //  새로운 Detection 객체 생성
        Detection detection = Detection.builder()
                .survivor(survivor)
                .detectionType(DetectionType.CCTV) // AI 비전 분석
                .cctv(cctv)
                .location(location)
                .detectedAt(now)
                .detectedStatus(mapPoseToStatus(humanDetection.getPose()))
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


    // Pose를 CurrentStatus로 매핑
    // AI 모델 클래스: ["Crawling", "Falling", "Sitting", "Standing"]
    private CurrentStatus mapPoseToStatus(String pose) {
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

    // 생존자 번호 생성 (자동 증가)
    private int generateSurvivorNumber() {
        Integer maxNumber = survivorRepository.findMaxSurvivorNumber();
        return maxNumber != null ? maxNumber + 1 : 1;
    }

     // 기존 생존자를 찾거나 새로 생성, 바운딩박스 중심점 거리 기반으로 가장 가까운 생존자 매칭 (이미 매칭된 생존자 제외)
    // 같은 CCTV에서 분석된 영상만 기존 생존자와 매칭, 다른 CCTV는 무조건 새 생존자 생성
    private Survivor findOrCreateSurvivor(AIDetectionResultDto.DetectionObject humanDetection,
                                           Location location,
                                           CCTV cctv,
                                           LocalDateTime now,
                                           java.util.Set<Long> matchedSurvivorIds) {

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
        final double DISTANCE_THRESHOLD = 100.0;    // 픽셀 단위 임계값 (조정가능) -> 100 픽셀 안에서 매칭되면 동일한 생존자로 간주
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

}