package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.dto.AIDetectionResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 환경 분석 서비스
 * 화재, 연기 등의 환경적 특성 분석
 */
@Service
@RequiredArgsConstructor
public class EnvironmentalAnalysisService {

    private final BoundingBoxAnalyzer boundingBoxAnalyzer;

    @Value("${spatial-analysis.screen.width:1920}")
    private int screenWidth;

    @Value("${spatial-analysis.screen.height:1080}")
    private int screenHeight;

    @Value("${spatial-analysis.thresholds.dense-smoke:0.5}")
    private double denseSmokeThreshold;

    @Value("${spatial-analysis.thresholds.large-fire:0.3}")
    private double largeFireThreshold;

    @Value("${spatial-analysis.thresholds.small-fire:0.05}")
    private double smallFireThreshold;

    @Value("${spatial-analysis.thresholds.fire-overlap-human:0.089}")
    private double fireOverlapHumanThreshold;

    /**
     * 생존자 바운딩 박스와 화재 바운딩 박스의 겹침 여부 확인
     * 화재가 생존자/침대에 직접 닿은 경우
     */
    public boolean checkFireOverlapHuman(AIDetectionResultDto.DetectionObject humanDetection,
                                          List<AIDetectionResultDto.DetectionObject> allDetections) {
        AIDetectionResultDto.BoundingBox humanBox = humanDetection.getBox();
        if (humanBox == null) return false;

        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("fire".equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox fireBox = detection.getBox();
                if (fireBox == null) continue;

                double overlapRatio = boundingBoxAnalyzer.calculateOverlapRatio(humanBox, fireBox);
                if (overlapRatio > fireOverlapHumanThreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 짙은 연기 감지 (smoke 박스 면적 합이 화면의 50% 이상)
     */
    public boolean checkDenseSmoke(List<AIDetectionResultDto.DetectionObject> allDetections) {
        int totalArea = screenWidth * screenHeight;
        int smokeTotalArea = calculateTotalAreaByClass(allDetections, "smoke");
        return (double) smokeTotalArea / totalArea >= denseSmokeThreshold;
    }

    /**
     * 방 전체로 화재 확산 (fire 박스 면적 합이 화면의 30% 이상)
     */
    public boolean checkLargeFireArea(List<AIDetectionResultDto.DetectionObject> allDetections) {
        int totalArea = screenWidth * screenHeight;
        int fireTotalArea = calculateTotalAreaByClass(allDetections, "fire");
        return (double) fireTotalArea / totalArea >= largeFireThreshold;
    }

    /**
     * 화재가 물체에 국한 (fire 박스 면적이 화면의 5% 미만)
     */
    public boolean checkSmallFire(List<AIDetectionResultDto.DetectionObject> allDetections) {
        int totalArea = screenWidth * screenHeight;
        int fireTotalArea = calculateTotalAreaByClass(allDetections, "fire");
        return fireTotalArea > 0 && (double) fireTotalArea / totalArea < smallFireThreshold;
    }

    /**
     * 특정 클래스의 바운딩 박스 면적 합 계산
     */
    private int calculateTotalAreaByClass(List<AIDetectionResultDto.DetectionObject> allDetections,
                                           String className) {
        int totalArea = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if (className.equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox box = detection.getBox();
                if (box != null) {
                    totalArea += boundingBoxAnalyzer.calculateBoxArea(box);
                }
            }
        }
        return totalArea;
    }
}