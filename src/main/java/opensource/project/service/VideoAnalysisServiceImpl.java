package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.CCTV;
import opensource.project.domain.Location;
import opensource.project.dto.VideoAnalysisRequestDto;
import opensource.project.dto.VideoAnalysisResponseDto;
import opensource.project.repository.CCTVRepository;
import opensource.project.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 로컬 동영상 파일 분석 서비스 구현체
 * FastAPI와 통신하여 로컬 동영상 파일 분석을 실행함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoAnalysisServiceImpl implements VideoAnalysisService {

    private final CCTVRepository cctvRepository;
    private final LocationRepository locationRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.object-detection.base-url}")
    private String fastApiBaseUrl;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    /**
     * 로컬 동영상 파일 분석을 시작함
     * CCTV, Location 정보를 검증한 후 FastAPI에 분석 요청을 보냄
     */
    @Override
    public VideoAnalysisResponseDto analyzeVideo(VideoAnalysisRequestDto requestDto) {
        log.info("Starting video analysis for file: {}, CCTV ID: {}, Location ID: {}",
                requestDto.getVideoPath(), requestDto.getCctvId(), requestDto.getLocationId());

        // CCTV 정보 조회 및 검증
        CCTV cctv = cctvRepository.findById(requestDto.getCctvId())
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + requestDto.getCctvId()));

        // Location 정보 조회 및 검증
        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + requestDto.getLocationId()));

        log.info("CCTV found: {}, Location found: {}", cctv.getCctvName(), location.getFullAddress());

        // FastAPI 요청 페이로드 구성 (JSON body 방식)
        Map<String, Object> payload = new HashMap<>();
        payload.put("video_path", requestDto.getVideoPath());
        payload.put("cctv_id", requestDto.getCctvId());
        payload.put("location_id", requestDto.getLocationId());

        if (requestDto.getConfThreshold() != null) {
            payload.put("conf_threshold", requestDto.getConfThreshold());
        }
        if (requestDto.getPoseConfThreshold() != null) {
            payload.put("pose_conf_threshold", requestDto.getPoseConfThreshold());
        }

        // FastAPI로 동영상 분석 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            String fastApiUrl = fastApiBaseUrl + "/analyze_video";
            log.info("Sending request to FastAPI: {}", fastApiUrl);
            log.info("Payload: {}", payload);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fastApiUrl,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                String message = (String) responseBody.get("message");

                // HLS URL 생성
                String hlsUrl = serverBaseUrl + "/streams/cctv" + requestDto.getCctvId() + "/playlist.m3u8";

                log.info("Video analysis started successfully. Status: {}, Message: {}", status, message);

                return VideoAnalysisResponseDto.builder()
                        .status(status)
                        .message(message != null ? message : "동영상 분석이 시작되었습니다")
                        .cctvId(requestDto.getCctvId())
                        .locationId(requestDto.getLocationId())
                        .videoPath(requestDto.getVideoPath())
                        .hlsUrl(hlsUrl)
                        .build();
            } else {
                throw new RuntimeException("FastAPI returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("Failed to start video analysis: {}", e.getMessage(), e);
            return VideoAnalysisResponseDto.builder()
                    .status("error")
                    .message("동영상 분석 시작에 실패했습니다: " + e.getMessage())
                    .cctvId(requestDto.getCctvId())
                    .locationId(requestDto.getLocationId())
                    .videoPath(requestDto.getVideoPath())
                    .build();
        }
    }
}