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
public class DetectionItemDto {
    @JsonProperty("class")
    private String objectClass;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("box")
    private BoundingBoxDto box;
}