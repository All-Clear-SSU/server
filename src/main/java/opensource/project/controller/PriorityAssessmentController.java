package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.PriorityAssessmentRequestDto;
import opensource.project.dto.PriorityAssessmentResponseDto;
import opensource.project.service.PriorityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/priority-assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PriorityAssessmentController {

    private final PriorityService priorityService;

    // 생존자에 대해 분석된 점수 목록 생성
    @PostMapping
    public ResponseEntity<PriorityAssessmentResponseDto> createPriorityAssessment(
            @Valid @RequestBody PriorityAssessmentRequestDto requestDto) {
        PriorityAssessmentResponseDto response = priorityService.createPriorityAssessment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 생존자에 대한 분석 점수 전달
    @GetMapping
    public ResponseEntity<List<PriorityAssessmentResponseDto>> getAllPriorityAssessments() {
        List<PriorityAssessmentResponseDto> assessments = priorityService.getAllPriorityAssessments();
        return ResponseEntity.ok(assessments);
    }

    // 특정 분석 결과(ID)에 대한 분석 점수 전달
    @GetMapping("/{id}")
    public ResponseEntity<PriorityAssessmentResponseDto> getPriorityAssessment(@PathVariable Long id) {
        PriorityAssessmentResponseDto assessment = priorityService.getPriorityAssessment(id);
        return ResponseEntity.ok(assessment);
    }

    // 특정 분석 결과(ID)에 대한 분석 점수 수정
    @PutMapping("/{id}")
    public ResponseEntity<PriorityAssessmentResponseDto> updatePriorityAssessment(
            @PathVariable Long id,
            @Valid @RequestBody PriorityAssessmentRequestDto requestDto) {
        PriorityAssessmentResponseDto response = priorityService.updatePriorityAssessment(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 특정 분석 결과(ID) 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePriorityAssessment(@PathVariable Long id) {
        priorityService.deletePriorityAssessment(id);
        return ResponseEntity.ok("Priorityassessment deleted successfully");
    }

    // 특정 생존자의 가장 최근 분석 점수 조회
    @GetMapping("/survivor/{survivorId}/latest")
    public ResponseEntity<PriorityAssessmentResponseDto> getLatestAssessmentForSurvivor(@PathVariable Long survivorId) {
        PriorityAssessmentResponseDto assessment = priorityService.getLatestAssessmentForSurvivor(survivorId);
        return ResponseEntity.ok(assessment);
    }

}
