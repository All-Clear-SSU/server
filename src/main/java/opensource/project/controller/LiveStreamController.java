package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.LiveStreamResponseDto;
import opensource.project.dto.LiveStreamStartRequestDto;
import opensource.project.dto.LiveStreamStatusResponseDto;
import opensource.project.service.LiveStreamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 라이브 스트리밍 컨트롤러
 * RTSP 라이브 스트리밍 제어 API를 제공함
 */
@Slf4j
@RestController
@RequestMapping("/live-stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Live Stream", description = "라이브 스트리밍 관리 API")
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    /**
     * 라이브 스트리밍을 시작함
     * CCTV의 RTSP 스트림을 FastAPI로 전송하여 AI 분석 및 HLS 변환을 시작함
     */
    @Operation(summary = "라이브 스트리밍 시작", description = "CCTV ID와 위치 ID를 받아 라이브 스트리밍을 시작합니다.")
    @PostMapping("/start")
    public ResponseEntity<LiveStreamResponseDto> startLiveStream(
            @Parameter(description = "CCTV ID", required = true)
            @RequestParam Long cctvId,

            @Parameter(description = "위치 ID", required = true)
            @RequestParam Long locationId,

            @Parameter(description = "객체 탐지 신뢰도 임계값 (기본값: 0.3)")
            @RequestParam(required = false) Double confThreshold,

            @Parameter(description = "자세 분류 신뢰도 임계값 (기본값: 0.3)")
            @RequestParam(required = false) Double poseConfThreshold) {

        log.info("=== Live Stream Start Request ===");
        log.info("CCTV ID: {}, Location ID: {}", cctvId, locationId);

        // RequestDto 생성
        LiveStreamStartRequestDto requestDto = new LiveStreamStartRequestDto(
                cctvId, locationId, confThreshold, poseConfThreshold
        );

        LiveStreamResponseDto response = liveStreamService.startLiveStream(requestDto);

        if ("success".equals(response.getStatus())) {
            log.info("Live stream started successfully. HLS URL: {}", response.getHlsUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            log.error("Failed to start live stream: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 라이브 스트리밍을 중지함
     * 진행 중인 스트리밍을 종료함
     */
    @Operation(summary = "라이브 스트리밍 중지", description = "CCTV ID로 진행 중인 라이브 스트리밍을 중지합니다.")
    @PostMapping("/stop/{cctvId}")
    public ResponseEntity<LiveStreamResponseDto> stopLiveStream(
            @Parameter(description = "CCTV ID", required = true)
            @PathVariable Long cctvId) {

        log.info("=== Live Stream Stop Request ===");
        log.info("CCTV ID: {}", cctvId);

        LiveStreamResponseDto response = liveStreamService.stopLiveStream(cctvId);

        if ("success".equals(response.getStatus())) {
            log.info("Live stream stopped successfully for CCTV ID: {}", cctvId);
            return ResponseEntity.ok(response);
        } else {
            log.error("Failed to stop live stream: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 라이브 스트리밍 상태를 조회함
     * 현재 스트리밍 진행 여부, 프레임 수, HLS URL 등을 반환함
     */
    @Operation(summary = "라이브 스트리밍 상태 조회", description = "CCTV ID로 현재 라이브 스트리밍 상태를 조회합니다.")
    @GetMapping("/status/{cctvId}")
    public ResponseEntity<LiveStreamStatusResponseDto> getStreamStatus(
            @Parameter(description = "CCTV ID", required = true)
            @PathVariable Long cctvId) {

        log.info("=== Live Stream Status Request ===");
        log.info("CCTV ID: {}", cctvId);

        LiveStreamStatusResponseDto response = liveStreamService.getStreamStatus(cctvId);
        return ResponseEntity.ok(response);
    }

    /**
     * HLS 재생 URL을 조회함
     * 스트리밍이 진행 중인 경우 HLS 플레이리스트 URL을 반환함
     */
    @Operation(summary = "HLS URL 조회", description = "CCTV ID로 HLS 재생 URL을 조회합니다.")
    @GetMapping("/url/{cctvId}")
    public ResponseEntity<String> getHlsUrl(
            @Parameter(description = "CCTV ID", required = true)
            @PathVariable Long cctvId) {

        log.info("=== HLS URL Request ===");
        log.info("CCTV ID: {}", cctvId);

        try {
            String hlsUrl = liveStreamService.getHlsUrl(cctvId);
            log.info("HLS URL: {}", hlsUrl);
            return ResponseEntity.ok(hlsUrl);
        } catch (IllegalStateException e) {
            log.error("Stream is not active: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}