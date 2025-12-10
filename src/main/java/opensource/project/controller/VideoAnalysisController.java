package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.VideoAnalysisRequestDto;
import opensource.project.dto.VideoAnalysisResponseDto;
import opensource.project.service.VideoAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 로컬 동영상 파일 분석 컨트롤러
 * 로컬에 저장된 동영상 파일 분석 API를 제공함
 */
@Slf4j
@RestController
@RequestMapping("/video-analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Video Analysis", description = "로컬 동영상 파일 분석 API")
public class VideoAnalysisController {

    private final VideoAnalysisService videoAnalysisService;

    /**
     * 로컬 동영상 파일 분석을 시작함
     * 동영상 파일 경로를 받아 FastAPI로 분석 요청을 보냄
     * 분석은 백그라운드에서 진행되며, 결과는 실시간으로 Spring Boot API로 전송됨
     */
    @Operation(
            summary = "로컬 동영상 파일 분석 시작",
            description = "로컬 동영상 파일 경로, CCTV ID, 위치 ID를 받아 AI 분석을 시작합니다. " +
                    "분석은 백그라운드에서 진행되며, HLS 스트리밍 URL을 통해 결과를 확인할 수 있습니다."
    )
    @PostMapping("/start")
    public ResponseEntity<VideoAnalysisResponseDto> startVideoAnalysis(
            @io.swagger.v3.oas.annotations.Parameter(description = "동영상 파일 경로 (예: /home/ubuntu/videos/sample.mp4)", required = true)
            @RequestParam String videoPath,

            @io.swagger.v3.oas.annotations.Parameter(description = "CCTV ID", required = true)
            @RequestParam Long cctvId,

            @io.swagger.v3.oas.annotations.Parameter(description = "위치 ID", required = true)
            @RequestParam Long locationId,

            @io.swagger.v3.oas.annotations.Parameter(description = "객체 탐지 신뢰도 임계값 (기본값: 0.3)")
            @RequestParam(required = false) Double confThreshold,

            @io.swagger.v3.oas.annotations.Parameter(description = "자세 분류 신뢰도 임계값 (기본값: 0.3)")
            @RequestParam(required = false) Double poseConfThreshold) {

        log.info("=== Video Analysis Start Request ===");
        log.info("Video Path: {}", videoPath);
        log.info("CCTV ID: {}, Location ID: {}", cctvId, locationId);

        // RequestDto 생성
        VideoAnalysisRequestDto requestDto = new VideoAnalysisRequestDto(
                videoPath, cctvId, locationId, confThreshold, poseConfThreshold
        );

        VideoAnalysisResponseDto response = videoAnalysisService.analyzeVideo(requestDto);

        if ("processing".equals(response.getStatus()) || "success".equals(response.getStatus())) {
            log.info("Video analysis started successfully. HLS URL: {}", response.getHlsUrl());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } else {
            log.error("Failed to start video analysis: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}