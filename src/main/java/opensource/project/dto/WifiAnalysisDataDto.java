package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * WiFi CSI (Channel State Information) 신호 분석 데이터를 담는 DTO
 * ESP32 모듈에서 수집한 CSI 신호를 AI 모델로 분석한 결과를 담음
 * Detection 엔티티의 rawData 필드에 JSON 형식으로 저장됨
 *
 * CSI는 WiFi 신호의 채널 상태 정보로, 실내 환경에서 사람의 움직임이나 존재를 감지하는 데 사용됨
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiAnalysisDataDto {

    /**
     * CSI 진폭(Amplitude) 데이터 (시계열)
     * 각 부반송파(subcarrier)의 신호 강도를 시간 순으로 저장함
     * 이차원 배열: [[subcarrier1의 시계열], [subcarrier2의 시계열], ...]
     * 예: [[120, 125, 130], [115, 118, 122], ...] → 그래프의 주 데이터
     */
    @JsonProperty("csi_amplitude")
    private List<List<Double>> csiAmplitude;

    /**
     * CSI 위상(Phase) 데이터 (시계열)
     * 각 부반송파의 위상 정보를 시간 순으로 저장함 (라디안 단위)
     * 위상 변화는 거리 변화를 나타내므로 움직임 감지에 중요함
     * 이차원 배열: [[subcarrier1의 위상], [subcarrier2의 위상], ...]
     */
    @JsonProperty("csi_phase")
    private List<List<Double>> csiPhase;

    /**
     * 부반송파(Subcarrier) 인덱스 목록
     * 802.11n/ac에서 사용하는 부반송파 번호를 저장함
     * 예: [0, 1, 2, 3, ..., 63] (64개 부반송파)
     * 그래프의 X축 또는 색상 구분에 사용됨
     */
    @JsonProperty("subcarrier_indices")
    private List<Integer> subcarrierIndices;

    /**
     * 타임스탬프 배열 (밀리초 단위)
     * CSI 샘플이 수집된 시각을 순서대로 저장함
     * 그래프의 시간축 렌더링에 사용됨
     * 예: [0, 100, 200, 300, ...] → 100ms 간격으로 샘플링
     */
    @JsonProperty("timestamps")
    private List<Long> timestamps;

    /**
     * RSSI (Received Signal Strength Indicator) 값
     * 전체 수신 신호 강도를 나타냄 (dBm 단위)
     * CSI보다 거친 정보이지만 전체적인 신호 세기 파악에 유용함
     */
    @JsonProperty("rssi")
    private Integer rssi;

    /**
     * 잡음 층위(Noise Floor) 값 (dBm 단위)
     * 환경 잡음 수준을 나타냄
     * RSSI와의 차이로 SNR(Signal-to-Noise Ratio)을 계산할 수 있음
     */
    @JsonProperty("noise_floor")
    private Integer noiseFloor;

    /**
     * 샘플링 레이트 (Hz 단위)
     * 초당 수집된 CSI 샘플 개수
     * 예: 100 → 초당 100개 샘플 (10ms 간격)
     */
    @JsonProperty("sampling_rate")
    private Integer samplingRate;

    /**
     * 안테나 개수 (MIMO 시스템)
     * ESP32가 사용하는 안테나 수를 나타냄
     * 예: 1 (단일 안테나), 2 (2x2 MIMO)
     */
    @JsonProperty("num_antennas")
    private Integer numAntennas;

    /**
     * WiFi 대역폭 (MHz 단위)
     * 사용된 채널 대역폭을 나타냄
     * 예: 20, 40, 80 (20MHz, 40MHz, 80MHz)
     * 대역폭이 넓을수록 더 많은 부반송파를 사용함
     */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    /**
     * 움직임 감지 여부
     * CSI 진폭 및 위상 변화를 분석하여 생존자의 움직임을 감지함
     */
    @JsonProperty("movement_detected")
    private Boolean movementDetected;

    /**
     * 움직임 강도 (0.0 ~ 1.0)
     * CSI 변화의 크기를 정규화한 값
     * 0에 가까울수록 정적, 1에 가까울수록 활발한 움직임
     */
    @JsonProperty("movement_intensity")
    private Double movementIntensity;

    /**
     * 호흡 감지 여부
     * CSI 미세 변화를 분석하여 생존자의 호흡을 감지함
     * 움직임이 없어도 호흡만으로 생존자 존재를 확인할 수 있음
     */
    @JsonProperty("breathing_detected")
    private Boolean breathingDetected;

    /**
     * 호흡률 (분당 호흡 횟수, BPM)
     * CSI 신호의 주기적 변화에서 추출한 호흡 속도
     * 정상 성인: 12~20 BPM
     */
    @JsonProperty("breathing_rate")
    private Double breathingRate;

    /**
     * 탐지된 사람 수 추정치
     * CSI 패턴 분석을 통해 추정한 인원 수
     * AI 모델의 분류 결과
     */
    @JsonProperty("estimated_people_count")
    private Integer estimatedPeopleCount;

    /**
     * CSI 주성분 분석(PCA) 결과
     * 고차원 CSI 데이터를 2~3개 주성분으로 축소한 값
     * 그래프 시각화 시 차원 축소에 사용됨
     * 예: [0.8, -0.3, 0.1] → 3차원 PCA
     */
    @JsonProperty("pca_components")
    private List<Double> pcaComponents;

    /**
     * AI 모델이 판단한 주요 탐지 특징
     * 생존자 존재를 판단하는 데 기여한 CSI 특징들
     * 예: "amplitude_variance", "phase_shift", "breathing_pattern"
     */
    @JsonProperty("detection_features")
    private List<String> detectionFeatures;

    /**
     * 원시 CSI 데이터의 통계 정보
     * 진폭의 평균, 표준편차, 최대/최소값 등을 포함함
     * 그래프의 Y축 범위 설정이나 이상치 탐지에 사용됨
     */
    @JsonProperty("amplitude_stats")
    private CsiStatistics amplitudeStats;

    /**
     * CSI 통계 정보를 담는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CsiStatistics {
        /**
         * 평균값
         */
        @JsonProperty("mean")
        private Double mean;

        /**
         * 표준편차
         */
        @JsonProperty("std_dev")
        private Double stdDev;

        /**
         * 최솟값
         */
        @JsonProperty("min")
        private Double min;

        /**
         * 최댓값
         */
        @JsonProperty("max")
        private Double max;

        /**
         * 중앙값
         */
        @JsonProperty("median")
        private Double median;
    }
}