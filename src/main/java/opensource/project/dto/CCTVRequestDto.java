package opensource.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import opensource.project.domain.enums.CCTVStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CCTVRequestDto {

    @NotNull(message = "카메라 번호는 필수입니다")
    private Integer cameraNumber;

    @NotBlank(message = "CCTV 코드는 필수입니다")
    private String cctvCode;

    private String cctvName;

    private String rtspUrl;

    @NotNull(message = "상태는 필수입니다")
    private CCTVStatus status;

    @NotNull(message = "위치 ID는 필수입니다")
    private Long locationId;

    @NotNull(message = "활성 여부는 필수입니다")
    private Boolean isActive;
}
