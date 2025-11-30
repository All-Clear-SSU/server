package opensource.project.service;

import opensource.project.dto.AIDetectionResultDto;
import org.springframework.stereotype.Component;

/**
 * 바운딩 박스 간의 겹침, 면적 계산 등 연산 담당
 */
@Component
public class BoundingBoxAnalyzer {

    /**
     * 두 바운딩 박스의 겹침 비율 계산 (IoU - Intersection over Union)
     *
     * @param box1 첫 번째 바운딩 박스
     * @param box2 두 번째 바운딩 박스
     * @return IoU 값 (0.0 ~ 1.0)
     */
    public double calculateOverlapRatio(AIDetectionResultDto.BoundingBox box1,
                                         AIDetectionResultDto.BoundingBox box2) {
        int x1 = Math.max(box1.getX1(), box2.getX1());
        int y1 = Math.max(box1.getY1(), box2.getY1());
        int x2 = Math.min(box1.getX2(), box2.getX2());
        int y2 = Math.min(box1.getY2(), box2.getY2());

        if (x2 <= x1 || y2 <= y1) {
            return 0.0;
        }

        int intersectionArea = (x2 - x1) * (y2 - y1);
        int box1Area = calculateBoxArea(box1);
        int box2Area = calculateBoxArea(box2);
        int unionArea = box1Area + box2Area - intersectionArea;

        return unionArea > 0 ? (double) intersectionArea / unionArea : 0.0;
    }

    /**
     * 바운딩 박스의 면적 계산
     *
     * @param box 바운딩 박스
     * @return 면적 (픽셀)
     */
    public int calculateBoxArea(AIDetectionResultDto.BoundingBox box) {
        if (box == null) {
            return 0;
        }
        return (box.getX2() - box.getX1()) * (box.getY2() - box.getY1());
    }

    /**
     * 두 바운딩 박스가 겹치는지 확인
     *
     * @param box1 첫 번째 바운딩 박스
     * @param box2 두 번째 바운딩 박스
     * @param threshold IoU 임계값 (0.0 ~ 1.0)
     * @return 겹침 여부
     */
    public boolean checkBoxOverlap(AIDetectionResultDto.BoundingBox box1,
                                    AIDetectionResultDto.BoundingBox box2,
                                    double threshold) {
        if (box1 == null || box2 == null) {
            return false;
        }
        return calculateOverlapRatio(box1, box2) > threshold;
    }
}