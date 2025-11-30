package opensource.project.service;

import opensource.project.dto.ObjectDetectionResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Object Detection API 클라이언트 인터페이스
 * 외부 AI 모델 API와의 통신
 */
public interface ObjectDetectionApiClient {

    /**
     * Object Detection API를 호출하여 탐지 결과를 받아옴
     *
     * @param imageFile 분석할 이미지 파일
     * @return Object Detection 결과
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    ObjectDetectionResultDto detectObjects(MultipartFile imageFile) throws IOException;

    /**
     * Object Detection API를 호출하여 분석된 이미지를 받아옴
     *
     * @param imageFile 분석할 이미지 파일
     * @return 분석된 이미지 바이트 배열
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    byte[] getAnalyzedImage(MultipartFile imageFile) throws IOException;
}