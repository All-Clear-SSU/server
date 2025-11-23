package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DetectionMethod {
    CCTV("CCTV"),
    WIFI("WiFi");

    private final String description;
}
