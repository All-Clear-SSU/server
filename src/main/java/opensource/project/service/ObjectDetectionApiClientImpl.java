package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.ObjectDetectionResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Object Detection API 클라이언트 구현체
 * RestTemplate을 사용하여 외부(FastAPI) AI 모델 API와 통신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectDetectionApiClientImpl implements ObjectDetectionApiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.object-detection.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${ai.object-detection.predict-endpoint:/predict}")
    private String predictEndpoint;

    @Value("${ai.object-detection.predict-image-endpoint:/predict_image}")
    private String predictImageEndpoint;

    @Value("${ai.object-detection.confidence-threshold:0.5}")
    private double confidenceThreshold;

    //Object Detection API를 호출하여 탐지 결과를 받아옴
    @Override
    public ObjectDetectionResultDto detectObjects(MultipartFile imageFile) throws IOException {
        String url = baseUrl + predictEndpoint + "?conf_threshold=" + confidenceThreshold;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ObjectDetectionResultDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    ObjectDetectionResultDto.class
            );

            ObjectDetectionResultDto result = response.getBody();
            if (result == null || result.getSummary() == null) {
                throw new RuntimeException("Object Detection API 응답이 비어있습니다");
            }

            log.info("Object Detection 완료 - fire: {}, human: {}, smoke: {}, total: {}",
                    result.getSummary().getFireCount(),
                    result.getSummary().getHumanCount(),
                    result.getSummary().getSmokeCount(),
                    result.getSummary().getTotalObjects());

            return result;
        } catch (Exception e) {
            log.error("Object Detection API 호출 실패", e);
            throw new RuntimeException("Object Detection 모델 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // Object Detection API를 호출하여 분석된 이미지를 받아옵니다.
    @Override
    public byte[] getAnalyzedImage(MultipartFile imageFile) throws IOException {
        String url = baseUrl + predictImageEndpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            byte[] imageBytes = response.getBody();
            if (imageBytes != null) {
                log.info("분석 이미지 획득 성공 (크기: {} bytes)", imageBytes.length);
                return imageBytes;
            } else {
                throw new RuntimeException("분석 이미지를 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("분석 이미지 API 호출 실패", e);
            throw new RuntimeException("분석 이미지 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }
}