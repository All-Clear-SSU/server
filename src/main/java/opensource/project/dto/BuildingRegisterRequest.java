package opensource.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BuildingRegisterRequest {

    @NotBlank
    private String buildingName;

    /**
     * CCTV 입력: RTSP URL과 위치 fullAddress만 받는다.
     */
    private List<CctvInput> cctvs;

    /**
     * WiFi(CSI) 입력: CSI 토픽과 위치 fullAddress만 받는다.
     */
    private List<WifiInput> wifiSensors;

    @Getter
    @Setter
    public static class CctvInput {
        @NotBlank
        private String rtspUrl;
        @NotBlank
        private String fullAddress;
    }

    @Getter
    @Setter
    public static class WifiInput {
        @NotBlank
        private String csiTopic;
        @NotBlank
        private String fullAddress;
    }
}
