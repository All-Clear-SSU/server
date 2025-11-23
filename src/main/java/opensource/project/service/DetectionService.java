package opensource.project.service;

import opensource.project.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 탐지 관리 서비스 인터페이스
 */
public interface DetectionService {

    // Detection 생성
    DetectionResponseDto createDetection(DetectionRequestDto requestDto);

    // 모든 Detection 조회
    List<DetectionResponseDto> getAllDetections();

    // 특정 생존자의 가장 최신 Detection 조회
    DetectionResponseDto getLatestDetectionBySurvivor(Long survivorId);

    // Detection 수정
    DetectionResponseDto updateDetection(Long id, DetectionRequestDto requestDto);

    // Detection 삭제
    void deleteDetection(Long id);

    // 특정 생존자의 종합 분석 정보 조회
    SurvivorAnalysisDto getSurvivorAnalysis(Long survivorId);

    // 이미지 분석 수행
    ImageAnalysisResponseDto analyzeImage(
            Long survivorId,
            Long locationId,
            Long cctvId,
            MultipartFile imageFile
    ) throws IOException;

    // Detection ID로 분석 이미지 조회
    byte[] getAnalyzedImage(Long detectionId);

    // AI 모델의 분석 결과를 처리
    void processAIDetectionResult(AIDetectionResultDto aiResult,
                                   Long cctvId,
                                   Long locationId,
                                   String videoUrl);
}
