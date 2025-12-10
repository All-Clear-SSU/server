package opensource.project.service;

import opensource.project.domain.enums.SensorStatus;
import opensource.project.dto.LocationRequestDto;
import opensource.project.dto.LocationResponseDto;
import opensource.project.dto.MemberRequestDto;
import opensource.project.dto.MemberResponseDto;
import opensource.project.dto.WifiSensorRequestDto;
import opensource.project.dto.WifiSensorResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BasicServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private WifiSensorService wifiSensorService;

    @Test
    @DisplayName("MemberService 기본 동작 테스트: 생성 및 조회")
    void testMemberServiceBasicOperations() {
        // given - 회원 생성 요청
        MemberRequestDto requestDto = new MemberRequestDto(1L, "테스트 회원");

        // when - 회원 생성
        MemberResponseDto createdMember = memberService.createMember(requestDto);

        // then - 생성 확인
        assertThat(createdMember).isNotNull();
        assertThat(createdMember.getId()).isEqualTo(1L);
        assertThat(createdMember.getName()).isEqualTo("테스트 회원");

        // when - 전체 회원 조회
        List<MemberResponseDto> allMembers = memberService.getAllMembers();

        // then - 조회 확인
        assertThat(allMembers).isNotEmpty();
        assertThat(allMembers).hasSize(1);
    }

    @Test
    @DisplayName("LocationService 기본 동작 테스트: 생성 및 조회")
    void testLocationServiceBasicOperations() {
        // given - 위치 생성 요청
        LocationRequestDto requestDto = LocationRequestDto.builder()
                .buildingName("본관")
                .floor(3)
                .roomNumber("301호")
                .build();

        // when - 위치 생성
        LocationResponseDto createdLocation = locationService.createLocation(requestDto);

        // then - 생성 확인
        assertThat(createdLocation).isNotNull();
        assertThat(createdLocation.getBuildingName()).isEqualTo("본관");
        assertThat(createdLocation.getFloor()).isEqualTo(3);
        assertThat(createdLocation.getRoomNumber()).isEqualTo("301호");

        // when - 전체 위치 조회
        List<LocationResponseDto> allLocations = locationService.getAllLocations();

        // then - 조회 확인
        assertThat(allLocations).isNotEmpty();
        assertThat(allLocations).hasSize(1);
    }

    @Test
    @DisplayName("WifiSensorService 기본 동작 테스트: 생성 및 조회")
    void testWifiSensorServiceBasicOperations() {
        // given - 먼저 위치 생성
        LocationRequestDto locationDto = LocationRequestDto.builder()
                .buildingName("신관")
                .floor(2)
                .roomNumber("201호")
                .build();
        LocationResponseDto location = locationService.createLocation(locationDto);

        // given - WiFi 센서 생성 요청
        WifiSensorRequestDto sensorDto = WifiSensorRequestDto.builder()
                .locationId(location.getId())
                .status(SensorStatus.ACTIVE)
                .signalStrength(-45)
                .detectionRadius(50.0)
                .isActive(true)
                .build();

        // when - WiFi 센서 생성
        WifiSensorResponseDto createdSensor = wifiSensorService.createWifiSensor(sensorDto);

        // then - 생성 확인
        assertThat(createdSensor).isNotNull();
        assertThat(createdSensor.getSensorCode()).isEqualTo("SENSOR-001");
        assertThat(createdSensor.getStatus()).isEqualTo(SensorStatus.ACTIVE);
        assertThat(createdSensor.getIsActive()).isTrue();

        // when - 전체 센서 조회
        List<WifiSensorResponseDto> allSensors = wifiSensorService.getAllWifiSensors(true);

        // then - 조회 확인
        assertThat(allSensors).isNotEmpty();
        assertThat(allSensors).hasSize(1);
    }

    @Test
    @DisplayName("통합 시나리오 테스트: 회원, 위치, 센서 순차 생성")
    void testIntegratedScenario() {
        // 1. 회원 생성
        MemberRequestDto memberDto = new MemberRequestDto(100L, "관리자");
        MemberResponseDto member = memberService.createMember(memberDto);
        assertThat(member).isNotNull();

        // 2. 위치 생성
        LocationRequestDto locationDto = LocationRequestDto.builder()
                .buildingName("중앙관")
                .floor(5)
                .roomNumber("501호")
                .build();
        LocationResponseDto location = locationService.createLocation(locationDto);
        assertThat(location).isNotNull();

        // 3. WiFi 센서 생성
        WifiSensorRequestDto sensorDto = WifiSensorRequestDto.builder()
                .locationId(location.getId())
                .status(SensorStatus.ACTIVE)
                .signalStrength(-50)
                .detectionRadius(30.0)
                .isActive(true)
                .build();
        WifiSensorResponseDto sensor = wifiSensorService.createWifiSensor(sensorDto);
        assertThat(sensor).isNotNull();

        // 4. 모든 데이터 조회 검증
        assertThat(memberService.getAllMembers()).isNotEmpty();
        assertThat(locationService.getAllLocations()).isNotEmpty();
        assertThat(wifiSensorService.getAllWifiSensors(true)).isNotEmpty();
    }
}