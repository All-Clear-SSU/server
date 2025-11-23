package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorRequestDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.service.SurvivorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/survivors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SurvivorController {

    private final SurvivorService survivorService;

    // 새로운 생존자 정보 등록
    @PostMapping
    public ResponseEntity<SurvivorResponseDto> createSurvivor(@Valid @RequestBody SurvivorRequestDto requestDto) {
        SurvivorResponseDto response = survivorService.createSurvivor(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    // 현재 모든 생존자 정보 조회
    @GetMapping
    public ResponseEntity<List<SurvivorResponseDto>> getAllSurvivors() {
        List<SurvivorResponseDto> survivors = survivorService.getAllSurvivors();
        return ResponseEntity.ok(survivors);
    }

    // 특정 생존자 정보 조회
    @GetMapping("/{id}")
    public ResponseEntity<SurvivorResponseDto> getSurvivor(@PathVariable Long id) {
        SurvivorResponseDto survivor = survivorService.getSurvivor(id);
        return ResponseEntity.ok(survivor);
    }

    // 특정 생존자 정보 변경
    @PutMapping("/{id}")
    public ResponseEntity<SurvivorResponseDto> updateSurvivor(
            @PathVariable Long id,
            @Valid @RequestBody SurvivorRequestDto requestDto) {
        SurvivorResponseDto response = survivorService.updateSurvivor(id, requestDto);
        return ResponseEntity.ok(response);
    }

    //'오탐(False Positive) 보고' 버튼 클릭 시 활용, 해당 생존자를 생존자 목록에서 제거
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSurvivor(@PathVariable Long id) {
        survivorService.deleteSurvivor(id);
        return ResponseEntity.ok("Survivor deleted successfully");
    }

    // '구조팀 파견' 버튼 클릭 시, 해당 생존자의 구조 상태가 '출동중'으로 변경
    @PatchMapping("/{id}/rescue-status")
    public ResponseEntity<SurvivorResponseDto> updateRescueStatus(
            @PathVariable Long id,
            @RequestParam RescueStatus rescueStatus) {
        SurvivorResponseDto response = survivorService.updateRescueStatus(id, rescueStatus);
        return ResponseEntity.ok(response);
    }

    // 어떤 탐지 수단으로부터 생존자가 발견되었는지 전달
    @GetMapping("/{id}/detection-method")
    public ResponseEntity<DetectionMethod> getDetectionMethod(@PathVariable Long id) {
        DetectionMethod detectionMethod = survivorService.getDetectionMethod(id);
        return ResponseEntity.ok(detectionMethod);
    }

    // 특정 생존자의 최신 AI 분석 점수 조회
    @GetMapping("/{id}/priority-score-latest")
    public ResponseEntity<PriorityScoreHistoryDto> getLatestPriorityScore(@PathVariable Long id) {
        PriorityScoreHistoryDto latestScore = survivorService.getLatestPriorityScore(id);
        return ResponseEntity.ok(latestScore);
    }
}
