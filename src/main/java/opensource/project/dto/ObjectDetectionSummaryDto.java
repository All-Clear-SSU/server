package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectDetectionSummaryDto {
    @JsonProperty("fire")
    private Integer fireCount;

    @JsonProperty("human")
    private Integer humanCount;

    @JsonProperty("smoke")
    private Integer smokeCount;

    @JsonProperty("total_objects")
    private Integer totalObjects;
}