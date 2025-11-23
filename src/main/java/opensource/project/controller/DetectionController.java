package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.AIAnalysisRequestDto;
import opensource.project.dto.DetectionRequestDto;
import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.ImageAnalysisResponseDto;
import opensource.project.dto.SurvivorAnalysisDto;
import opensource.project.service.DetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/detections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DetectionController {

    private final DetectionService detectionService;

    // Detection 추가
    @PostMapping
    public ResponseEntity<DetectionResponseDto> createDetection(@Valid @RequestBody DetectionRequestDto requestDto) {
        DetectionResponseDto response = detectionService.createDetection(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 Detection 정보 조회
    @GetMapping
    public ResponseEntity<List<DetectionResponseDto>> getAllDetections() {
        List<DetectionResponseDto> detections = detectionService.getAllDetections();
        return ResponseEntity.ok(detections);
    }

    // 특정 생존자의 가장 최신 Detection 조회
    @GetMapping("/survivor/{survivorId}/latest")
    public ResponseEntity<DetectionResponseDto> getLatestDetectionBySurvivor(@PathVariable Long survivorId) {
        DetectionResponseDto response = detectionService.getLatestDetectionBySurvivor(survivorId);
        return ResponseEntity.ok(response);
    }

    // 특정 Detection 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<DetectionResponseDto> updateDetection(
            @PathVariable Long id,
            @Valid @RequestBody DetectionRequestDto requestDto) {
        DetectionResponseDto response = detectionService.updateDetection(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 특정 Detection 정보 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDetection(@PathVariable Long id) {
        detectionService.deleteDetection(id);
        return ResponseEntity.ok("Detection deleted successfully");
    }

    /**
     * 특정 생존자의 종합 분석 정보 조회
     * 반환 정보:
     * - aiAnalysisResult: AI 분석 상태 메시지
     * - fullAddress: 위치
     * - currentStatus: 생존자 현재 상태
     * - detectionMethod: 탐지 수단
     *  - confidenceCoefficient: 신뢰도 계수
     * - statusScore, environmentScore, finalRiskScore: 위험도 점수 관련
     */
    @GetMapping("/survivor/{survivorId}/analysis")
    public ResponseEntity<SurvivorAnalysisDto> getSurvivorAnalysis(@PathVariable Long survivorId) {
        SurvivorAnalysisDto response = detectionService.getSurvivorAnalysis(survivorId);
        return ResponseEntity.ok(response);
    }

    /**
     * ML 모델을 호출하여 이미지를 분석하고, Detection과 PriorityAssessment를 생성
     *
     * 요청 파라미터:
     * - file: 분석할 이미지 파일 (MultipartFile)
     * - survivorId: 생존자 ID
     * - locationId: 위치 ID
     * - cctvId: CCTV ID
     *
     * 반환 정보:
     * - fireCount, humanCount, smokeCount, totalObjects: Object Detection 결과
     * - survivorStatusMessage: 생존자 상태메시지
     * - statusScore, environmentScore, confidenceCoefficient, finalRiskScore: 우선순위 점수 (더미 데이터)
     * - analyzedImageBase64: Base64 인코딩된 분석 이미지
     * - detectionId, priorityAssessmentId: 생성된 엔터티 ID
     */
    @Operation(summary = "이미지 분석", description = "이미지를 업로드하여 ML 모델로 분석하고 결과를 저장합니다.")
    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageAnalysisResponseDto> analyzeImage(
            @Parameter(description = "분석할 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "생존자 ID", required = true)
            @RequestParam("survivorId") Long survivorId,
            @Parameter(description = "위치 ID", required = true)
            @RequestParam("locationId") Long locationId,
            @Parameter(description = "CCTV ID", required = true)
            @RequestParam("cctvId") Long cctvId
    ) {
        try {
            ImageAnalysisResponseDto response = detectionService.analyzeImage(
                    survivorId,
                    locationId,
                    cctvId,
                    file
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new RuntimeException("이미지 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // Detection ID로 저장된 분석 이미지를 조회, 이미지 파일을 직접 반환하므로 브라우저나 Swagger에서 바로 확인가능
    @Operation(summary = "분석 이미지 조회", description = "Detection ID로 분석된 이미지 파일을 조회합니다.")
    @GetMapping(value = "/{detectionId}/analyzed-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAnalyzedImage(
            @Parameter(description = "Detection ID", required = true)
            @PathVariable Long detectionId
    ) {
        byte[] imageBytes = detectionService.getAnalyzedImage(detectionId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    // FastAPI의 /analyze_video 엔드포인트(API)에서 프레임별 분석 결과를 전송받아 처리
    @Operation(summary = "AI 분석 결과 수신", description = "FastAPI로부터 영상 분석 결과를 받아 Survivor, Detection, PriorityAssessment를 생성합니다.")
    @PostMapping("/ai-analysis")
    public ResponseEntity<String> receiveAIAnalysis(@Valid @RequestBody AIAnalysisRequestDto request) {
        log.info("========================================");
        log.info("=== AI Analysis Request Received ===");
        log.info("========================================");
        log.info("CCTV ID: {}", request.getCctvId());
        log.info("Location ID: {}", request.getLocationId());
        log.info("Video URL: {}", request.getVideoUrl());

        int detectionCount = 0;
        int humanCount = 0;
        if (request.getAiResult() != null) {
            if (request.getAiResult().getDetections() != null) {
                detectionCount = request.getAiResult().getDetections().size();  // 전체 탐지 객체 수 detectionCount에 저장
                humanCount = (int) request.getAiResult().getDetections().stream()   // Stream으로 변환
                        .filter(d -> "human".equalsIgnoreCase(d.getClassName()))    // human만 필터링
                        .count();   // 탐지된 human 수 human Count에 저장
            }
            // 객체 탐지 결과 로그에 남기기
            if (request.getAiResult().getSummary() != null) {
                log.info("Summary - Fire: {}, Human: {}, Smoke: {}, Total: {}",
                        request.getAiResult().getSummary().getFireCount(),
                        request.getAiResult().getSummary().getHumanCount(),
                        request.getAiResult().getSummary().getSmokeCount(),
                        request.getAiResult().getSummary().getTotalObjects());
            }
        }

        log.info("Total Detections: {}, Human Detections: {}", detectionCount, humanCount);
        log.info("Processing AI detection result...");

        detectionService.processAIDetectionResult(
                request.getAiResult(),
                request.getCctvId(),
                request.getLocationId(),
                request.getVideoUrl()
        );

        log.info("AI analysis processing completed");
        log.info("========================================");

        return ResponseEntity.ok("AI analysis processed successfully");
    }
}
