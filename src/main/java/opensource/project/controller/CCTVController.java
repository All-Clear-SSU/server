package opensource.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.CCTVRequestDto;
import opensource.project.dto.CCTVResponseDto;
import opensource.project.dto.StreamInfoDto;
import opensource.project.dto.UpdateRtspUrlRequestDto;
import opensource.project.service.CCTVService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cctvs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "CCTV", description = "CCTV 관리 API")
public class CCTVController {

    private final CCTVService cctvService;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    // CCTV 추가
    @PostMapping
    public ResponseEntity<CCTVResponseDto> createCCTV(@Valid @RequestBody CCTVRequestDto requestDto) {
        CCTVResponseDto response = cctvService.createCCTV(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 CCTV 조회
    @GetMapping
    public ResponseEntity<List<CCTVResponseDto>> getAllCCTVs() {
        List<CCTVResponseDto> cctvs = cctvService.getAllCCTVs();
        return ResponseEntity.ok(cctvs);
    }

    // 특정 CCTV 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<CCTVResponseDto> updateCCTV(
            @PathVariable Long id,
            @Valid @RequestBody CCTVRequestDto requestDto) {
        CCTVResponseDto response = cctvService.updateCCTV(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 특정 CCTV 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCCTV(@PathVariable Long id) {
        cctvService.deleteCCTV(id);
        return ResponseEntity.ok("CCTV deleted successfully");
    }

    /**
     * CCTV의 RTSP URL만 업데이트함
     * 사용자가 자신의 휴대폰 Tailscale RTSP URL을 등록할 때 사용
     */
    @Operation(summary = "RTSP URL 업데이트", description = "CCTV ID로 해당 CCTV의 RTSP URL만 업데이트합니다.")
    @PatchMapping("/{id}/rtsp-url")
    public ResponseEntity<CCTVResponseDto> updateRtspUrl(
            @Parameter(description = "CCTV ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateRtspUrlRequestDto requestDto) {
        CCTVResponseDto response = cctvService.updateRtspUrl(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // CCTV별 HLS 스트림 URL 제공
    @GetMapping("/streams/{cctvId}")
    public ResponseEntity<StreamInfoDto> getStream(@PathVariable Long cctvId) {
        // CCTV 존재 확인
        cctvService.getCCTV(cctvId);

        // HLS 스트림 URL 반환 (환경변수로 설정된 서버 URL 사용)
        String streamUrl = String.format("%s/streams/cctv%d/playlist.m3u8", serverBaseUrl, cctvId);

        return ResponseEntity.ok(new StreamInfoDto(streamUrl));
    }
}