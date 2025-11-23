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
public class BoundingBoxDto {
    @JsonProperty("x1")
    private Integer x1;

    @JsonProperty("y1")
    private Integer y1;

    @JsonProperty("x2")
    private Integer x2;

    @JsonProperty("y2")
    private Integer y2;
}