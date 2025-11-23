package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurrentStatus {
    FALLING("쓰러져 있음"),
    CRAWLING("기어가고 있음"),
    SITTING("앉아 있음"),
    STANDING("서 있음");

    private final String description;
}
