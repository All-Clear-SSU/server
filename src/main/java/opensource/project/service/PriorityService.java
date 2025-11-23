package opensource.project.service;

import opensource.project.domain.Detection;
import opensource.project.domain.PriorityAssessment;
import opensource.project.domain.Survivor;
import opensource.project.dto.AIDetectionResultDto;
import opensource.project.dto.PriorityAssessmentRequestDto;
import opensource.project.dto.PriorityAssessmentResponseDto;

import java.util.List;

/**
 * 우선순위계산 서비스 인터페이스
 */
public interface PriorityService {

    // 생존자의 분석 점수 생성
    PriorityAssessmentResponseDto createPriorityAssessment(PriorityAssessmentRequestDto requestDto);

    // 모든 생존자의 분석 점수 반환
    List<PriorityAssessmentResponseDto> getAllPriorityAssessments();

    // 특정 생존자의 분석 점수 반환
    PriorityAssessmentResponseDto getPriorityAssessment(Long id);

    // 분석 점수 수정
    PriorityAssessmentResponseDto updatePriorityAssessment(Long id, PriorityAssessmentRequestDto requestDto);

    // 특정 생존자의 분석 점수 삭제
    void deletePriorityAssessment(Long id);

    // 특정 생존자의 가장 최근 분석 점수 가져오기
    PriorityAssessmentResponseDto getLatestAssessmentForSurvivor(Long survivorId);

    // 더미 PriorityAssessment 데이터를 생성
    PriorityAssessmentRequestDto createDummyPriorityAssessment(Long detectionId, Long survivorId);

    // AI 모델 분석 결과 기반으로 PriorityAssessment 생성 및 저장
    PriorityAssessment createAssessmentFromAI(
            AIDetectionResultDto.DetectionObject humanDetection,
            List<AIDetectionResultDto.DetectionObject> allDetections,
            AIDetectionResultDto.DetectionSummary summary,
            Survivor survivor,
            Detection detection);
}