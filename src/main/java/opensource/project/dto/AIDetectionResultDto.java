package opensource.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIDetectionResultDto {

    private String imagePath;
    private List<DetectionObject> detections;
    private DetectionSummary summary;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionObject {
        private String className;  // "fire", "human", "smoke"
        private Double confidence;
        private BoundingBox box;
        private String pose;       // "Fall", "Standing", "Sitting" (human only)
        private Double poseScore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private Integer x1;
        private Integer y1;
        private Integer x2;
        private Integer y2;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionSummary {
        private Integer fireCount;
        private Integer humanCount;
        private Integer smokeCount;
        private Integer totalObjects;
    }
}