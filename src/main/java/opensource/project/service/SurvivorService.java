package opensource.project.service;

import opensource.project.domain.enums.DeleteReason;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorRequestDto;
import opensource.project.dto.SurvivorResponseDto;

import java.util.List;

/**
 * 생존자 관리 서비스 인터페이스
 */
public interface SurvivorService {

    // 새로운 생존자 정보 등록
    SurvivorResponseDto createSurvivor(SurvivorRequestDto requestDto);

    // 생존자 엔터티의 모든 인스턴스 반환
    List<SurvivorResponseDto> getAllSurvivors();

    // 생존자 엔터티의 특정 인스턴스 반환
    SurvivorResponseDto getSurvivor(Long id);

    // 생존자 정보 수정
    SurvivorResponseDto updateSurvivor(Long id, SurvivorRequestDto requestDto);

    // 생존자 목록에서 해당 id의 생존자를 제거
    void deleteSurvivor(Long id, DeleteReason reason);

    // 구조 상태 변경
    SurvivorResponseDto updateRescueStatus(Long id, RescueStatus rescueStatus);

    // 탐지된 수단 찾기
    DetectionMethod getDetectionMethod(Long id);

    // 가장 최근 분석 점수 조회
    PriorityScoreHistoryDto getLatestPriorityScore(Long id);
}
