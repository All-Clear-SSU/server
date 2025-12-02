package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.CCTV;
import opensource.project.dto.LiveStreamResponseDto;
import opensource.project.dto.LiveStreamStartRequestDto;
import opensource.project.dto.LiveStreamStatusResponseDto;
import opensource.project.repository.CCTVRepository;
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
 * 라이브 스트리밍 서비스 구현체
 * FastAPI와 통신하여 RTSP 라이브 스트리밍을 제어함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamServiceImpl implements LiveStreamService {

    private final CCTVRepository cctvRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.object-detection.base-url}")
    private String fastApiBaseUrl;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    /**
     * 라이브 스트리밍을 시작함
     * CCTV 정보를 조회하여 FastAPI에 RTSP 스트리밍 시작 요청을 보냄
     */
    @Override
    public LiveStreamResponseDto startLiveStream(LiveStreamStartRequestDto requestDto) {
        log.info("Starting live stream for CCTV ID: {}, Location ID: {}",
                requestDto.getCctvId(), requestDto.getLocationId());

        // CCTV 정보 조회
        CCTV cctv = cctvRepository.findById(requestDto.getCctvId())
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + requestDto.getCctvId()));

        // RTSP URL 확인
        String rtspUrl = cctv.getRtspUrl();
        if (rtspUrl == null || rtspUrl.isEmpty()) {
            throw new IllegalArgumentException("CCTV does not have RTSP URL configured");
        }

        // FastAPI 요청 페이로드 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("rtsp_url", rtspUrl);
        payload.put("cctv_id", requestDto.getCctvId());
        payload.put("location_id", requestDto.getLocationId());

        if (requestDto.getConfThreshold() != null) {
            payload.put("conf_threshold", requestDto.getConfThreshold());
        }
        if (requestDto.getPoseConfThreshold() != null) {
            payload.put("pose_conf_threshold", requestDto.getPoseConfThreshold());
        }

        // FastAPI로 스트리밍 시작 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            String fastApiUrl = fastApiBaseUrl + "/start_live_stream";
            log.info("Sending request to FastAPI: {}", fastApiUrl);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fastApiUrl,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String hlsUrl = (String) responseBody.get("hls_url");

                log.info("Live stream started successfully. HLS URL: {}", hlsUrl);

                return LiveStreamResponseDto.builder()
                        .status("success")
                        .message("라이브 스트리밍이 시작되었습니다")
                        .cctvId(requestDto.getCctvId())
                        .hlsUrl(hlsUrl)
                        .rtspUrl(rtspUrl)
                        .build();
            } else {
                throw new RuntimeException("FastAPI returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("Failed to start live stream: {}", e.getMessage(), e);
            return LiveStreamResponseDto.builder()
                    .status("error")
                    .message("라이브 스트리밍 시작에 실패했습니다: " + e.getMessage())
                    .cctvId(requestDto.getCctvId())
                    .build();
        }
    }

    /**
     * 라이브 스트리밍을 중지함
     * FastAPI에 스트리밍 중지 요청을 보냄
     */
    @Override
    public LiveStreamResponseDto stopLiveStream(Long cctvId) {
        log.info("Stopping live stream for CCTV ID: {}", cctvId);

        // CCTV 존재 확인
        CCTV cctv = cctvRepository.findById(cctvId)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + cctvId));

        // FastAPI 요청 페이로드 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("cctv_id", cctvId);

        // FastAPI로 스트리밍 중지 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            String fastApiUrl = fastApiBaseUrl + "/stop_live_stream";
            log.info("Sending stop request to FastAPI: {}", fastApiUrl);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fastApiUrl,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Live stream stopped successfully for CCTV ID: {}", cctvId);

                return LiveStreamResponseDto.builder()
                        .status("success")
                        .message("라이브 스트리밍이 중지되었습니다")
                        .cctvId(cctvId)
                        .build();
            } else {
                throw new RuntimeException("FastAPI returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("Failed to stop live stream: {}", e.getMessage(), e);
            return LiveStreamResponseDto.builder()
                    .status("error")
                    .message("라이브 스트리밍 중지에 실패했습니다: " + e.getMessage())
                    .cctvId(cctvId)
                    .build();
        }
    }

    /**
     * 라이브 스트리밍 상태를 조회함
     * FastAPI에서 스트리밍 상태를 가져옴
     */
    @Override
    public LiveStreamStatusResponseDto getStreamStatus(Long cctvId) {
        log.info("Getting stream status for CCTV ID: {}", cctvId);

        // CCTV 정보 조회
        CCTV cctv = cctvRepository.findById(cctvId)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + cctvId));

        try {
            String fastApiUrl = fastApiBaseUrl + "/stream_status/" + cctvId;
            log.info("Requesting stream status from FastAPI: {}", fastApiUrl);

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    fastApiUrl,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                Boolean isStreaming = (Boolean) responseBody.get("is_streaming");
                String rtspUrl = (String) responseBody.get("rtsp_url");
                String startedAt = (String) responseBody.get("started_at");
                Integer frameCount = (Integer) responseBody.get("frame_count");

                String hlsUrl = null;
                if (Boolean.TRUE.equals(isStreaming)) {
                    hlsUrl = serverBaseUrl + "/streams/cctv" + cctvId + "/playlist.m3u8";
                }

                return LiveStreamStatusResponseDto.builder()
                        .cctvId(cctvId)
                        .isStreaming(isStreaming)
                        .rtspUrl(rtspUrl)
                        .hlsUrl(hlsUrl)
                        .startedAt(startedAt)
                        .frameCount(frameCount)
                        .cctvName(cctv.getCctvName())
                        .location(cctv.getLocation() != null ? cctv.getLocation().getFullAddress() : null)
                        .build();
            } else {
                throw new RuntimeException("FastAPI returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("Failed to get stream status: {}", e.getMessage(), e);

            // 스트리밍 중이 아닌 상태로 반환
            return LiveStreamStatusResponseDto.builder()
                    .cctvId(cctvId)
                    .isStreaming(false)
                    .cctvName(cctv.getCctvName())
                    .location(cctv.getLocation() != null ? cctv.getLocation().getFullAddress() : null)
                    .build();
        }
    }

    /**
     * HLS 재생 URL을 반환함
     * 스트리밍이 진행 중인 경우에만 유효한 URL을 반환
     */
    @Override
    public String getHlsUrl(Long cctvId) {
        log.info("Getting HLS URL for CCTV ID: {}", cctvId);

        // CCTV 존재 확인
        cctvRepository.findById(cctvId)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found: " + cctvId));

        // 스트리밍 상태 확인
        LiveStreamStatusResponseDto status = getStreamStatus(cctvId);

        if (Boolean.TRUE.equals(status.getIsStreaming())) {
            return serverBaseUrl + "/streams/cctv" + cctvId + "/playlist.m3u8";
        } else {
            throw new IllegalStateException("Live stream is not active for CCTV ID: " + cctvId);
        }
    }
}