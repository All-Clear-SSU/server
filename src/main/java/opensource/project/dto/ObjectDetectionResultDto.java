package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectDetectionResultDto {
    @JsonProperty("image_path")
    private String imagePath;

    @JsonProperty("detections")
    private List<DetectionItemDto> detections;

    @JsonProperty("summary")
    private ObjectDetectionSummaryDto summary;
}