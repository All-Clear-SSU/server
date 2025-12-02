package opensource.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RTSP URL 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRtspUrlRequestDto {

    /**
     * RTSP URL (필수)
     * rtsp:// 로 시작해야 함
     */
    @NotBlank(message = "RTSP URL은 필수입니다")
    @Pattern(regexp = "^rtsp://.*", message = "RTSP URL은 rtsp://로 시작해야 합니다")
    private String rtspUrl;
}