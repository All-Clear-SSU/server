package opensource.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequestDto {

    @NotBlank(message = "건물명은 필수입니다")
    private String buildingName;

    @NotNull(message = "층수는 필수입니다")
    private Integer floor;

    private String roomNumber;
}
