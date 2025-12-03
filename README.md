# All-Clear 프로젝트 백엔드 API

재난 상황에서 생존자를 탐지하고 위험도를 평가하는 AI 기반 실시간 모니터링 시스템

## 목차
1. [서버 실행 방법](#1-서버-실행-방법)
2. [파일 구조](#2-파일-구조)
3. [PATCH /cctvs/{id}/rtsp-url 사용법](#3-patch-cctvsidrtsp-url-사용법)
4. [Live-streaming 관련 API 사용법](#4-live-streaming-관련-api-사용법)
5. [Swagger 문서 주소와 사용법](#5-swagger-문서-주소와-사용법)
6. [실시간 스트리밍 진행시 생존자 탐지 및 탐지정보, 위험도 점수 업데이트되는 과정](#6-실시간-스트리밍-진행시-생존자-탐지-및-위험도-점수-업데이트-과정)
7. [WiFi CSI 센서 기반 생존자 탐지 과정 및 관련 API](#7-wifi-csi-센서-기반-생존자-탐지-시스템)
---

## 1. 서버 실행 과정

#### 환경 변수 목록
다음 환경 변수가 설정 되어있음:
```bash
export DB_URL (ex.jdbc:oracle:thin:@(오라클DB_URL):(포트)/(DB이름))
export DB_USERNAME
export DB_PASSWORD
export SERVER_BASE_URL (http://(EC2서버주소):8080)
export AI_API_BASE_URL (http://(EC2서버주소):8000)
export MQTT_ENABLED=false  # (선택사항, 기본값: false, mqtt 테스트시 true로 변경)
```

#### 요구 사항
- Java 21
- Gradle 8.x
- Oracle Database
- FastAPI AI 서버 (생존자 탐지 및 HLS 스트리밍 변환용)


#### 자동 배포 (GitHub Actions)
`main` 브랜치에 푸시하면 자동으로 EC2 인스턴스에 배포됩니다.
- `.github/workflows/deploy.yml` 워크플로우가 실행됩니다
- 필요한 GitHub Secrets: `EC2_SSH_KEY`, `EC2_HOST`, `EC2_USER`
- Spring Boot, FastAPI 서버가 실행됨

### 서버 실행중 확인
서버가 정상적으로 실행되면 다음 주소로 접근할 수 있습니다:
- Swagger UI(Spring Boot): http://(server_url):8080/swagger-ui.html
- Swagger UI(FastAPI): http://(server_url):8000/dcos

---

## 2. 파일 구조

### 프로젝트 레이아웃
```
project/
├── src/
│   ├── main/
│   │   ├── java/opensource/project/
│   │   │   ├── config/              # 설정 클래스
│   │   │   │   ├── OpenApiConfig.java       # Swagger/OpenAPI 설정
│   │   │   │   ├── WebConfig.java           # CORS 설정
│   │   │   │   └── WebSocketConfig.java     # WebSocket 설정
│   │   │   ├── controller/          # REST API 컨트롤러
│   │   │   │   ├── CCTVController.java
│   │   │   │   ├── DetectionController.java
│   │   │   │   ├── LiveStreamController.java
│   │   │   │   ├── LocationController.java
│   │   │   │   ├── MemberController.java
│   │   │   │   ├── PriorityAssessmentController.java
│   │   │   │   ├── SurvivorController.java
│   │   │   │   ├── WebSocketTestController.java
│   │   │   │   ├── WifiDetectionController.java   # WiFi 탐지 테스트용
│   │   │   │   └── WifiSensorController.java      # WiFi 센서 CRUD
│   │   │   ├── domain/              # JPA 엔티티
│   │   │   │   ├── enums/           # Enum 클래스
│   │   │   │   ├── CCTV.java
│   │   │   │   ├── Detection.java
│   │   │   │   ├── Location.java
│   │   │   │   ├── Member.java
│   │   │   │   ├── PriorityAssessment.java
│   │   │   │   ├── Survivor.java
│   │   │   │   └── WifiSensor.java
│   │   │   ├── dto/                 # 데이터 전송 객체
│   │   │   │   ├── CCTVRequestDto.java
│   │   │   │   ├── CCTVResponseDto.java
│   │   │   │   ├── LiveStreamStartRequestDto.java
│   │   │   │   ├── LiveStreamResponseDto.java
│   │   │   │   ├── UpdateRtspUrlRequestDto.java
│   │   │   │   └── ... (기타 DTO)
│   │   │   ├── repository/          # Spring Data JPA 리포지토리
│   │   │   │   ├── CCTVRepository.java
│   │   │   │   ├── DetectionRepository.java
│   │   │   │   ├── LocationRepository.java
│   │   │   │   ├── SurvivorRepository.java
│   │   │   │   └── ... (기타 Repository)
│   │   │   └── service/             # 비즈니스 로직 서비스
│   │   │       ├── # CCTV 및 AI 탐지 관련
│   │   │       ├── AIDetectionProcessorService.java      # CCTV AI 탐지 결과 처리 및 생존자 매칭
│   │   │       ├── ObjectDetectionApiClient.java         # 외부 AI API 통신 인터페이스
│   │   │       ├── ObjectDetectionApiClientImpl.java     # FastAPI AI 서버 HTTP 통신
│   │   │       ├── LiveStreamService.java                # 라이브 스트리밍 제어 인터페이스
│   │   │       ├── LiveStreamServiceImpl.java            # RTSP → HLS 변환 및 AI 분석
│   │   │       ├── CCTVService.java / CCTVServiceImpl.java
│   │   │       │
│   │   │       ├── # WiFi 센서 탐지 관련
│   │   │       ├── WifiDetectionMqttService.java         # MQTT 메시지 수신 및 처리
│   │   │       ├── WifiDetectionProcessorService.java    # WiFi 생존자 매칭 및 DB 저장
│   │   │       ├── WifiSensorService.java / WifiSensorServiceImpl.java
│   │   │       ├── WifiDetectionService.java             # WiFi 탐지 데이터 조회 서비스
│   │   │       │
│   │   │       ├── # 생존자 및 위험도 평가 관련
│   │   │       ├── SurvivorService.java / SurvivorServiceImpl.java    # 생존자 CRUD
│   │   │       ├── SurvivorMatchingService.java          # 바운딩박스 기반 생존자 매칭
│   │   │       ├── PriorityService.java / PriorityServiceImpl.java    # 위험도 평가 종합
│   │   │       ├── RiskScoreCalculator.java              # 위험도 점수 계산 알고리즘
│   │   │       ├── EnvironmentalAnalysisService.java     # 환경 위험 요소 분석
│   │   │       ├── BoundingBoxAnalyzer.java              # 바운딩박스 IoU 계산
│   │   │       │
│   │   │       ├── # 기타 도메인 서비스
│   │   │       ├── DetectionService.java / DetectionServiceImpl.java  # 탐지 기록 관리
│   │   │       ├── LocationService.java / LocationServiceImpl.java    # 위치 정보 관리
│   │   │       ├── MemberService.java / MemberServiceImpl.java        # 회원 관리
│   │   │       │
│   │   │       └── # 실시간 통신
│   │   │           ├── WebSocketService.java             # WebSocket 브로드캐스트 인터페이스
│   │   │           └── WebSocketServiceImpl.java         # 실시간 알림 전송
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정
│   │       └── application.properties
│   └── test/                        # 테스트 코드
├── build.gradle                     # Gradle 빌드 설정
├── CLAUDE.md                        # AI 어시스턴트용 프로젝트 가이드
└── README.md                        # 프로젝트 문서 (본 파일)
```

### 아키텍처 레이어

**Controller Layer** (`controller/`)
- HTTP 요청/응답을 처리하고 서비스 레이어에 위임
- `@RestController`, `@RequestMapping` 사용
- Bean Validation을 통한 입력 검증

**Service Layer** (`service/`)
- 비즈니스 로직 및 트랜잭션 관리
- `@Service`, `@Transactional` 사용
- 도메인 로직 구현 및 외부 API 통신

**Repository Layer** (`repository/`)
- Spring Data JPA를 사용한 데이터 접근
- `JpaRepository` 인터페이스 확장

**Domain Layer** (`domain/`)
- JPA 엔티티 클래스
- 데이터베이스 테이블과 매핑
- Lombok을 사용한 보일러플레이트 코드 감소

**DTO Layer** (`dto/`)
- 계층 간 데이터 전송 객체
- Request/Response DTO 분리
- Bean Validation 애노테이션 사용

---

## 3. PATCH /cctvs/{id}/rtsp-url 사용법

### 개요
CCTV의 RTSP URL만 업데이트하는 API입니다. 사용자가 자신의 휴대폰이나 외부 CCTV의 Tailscale RTSP URL을 등록할 때 사용합니다.

### 엔드포인트
```
PATCH /cctvs/{id}/rtsp-url
```

### 파라미터
- **Path Variable**
  - `id` (Long, required): 업데이트할 CCTV의 ID

- **Request Body** (JSON)
```json
{
  "rtspUrl": "rtsp://your-rtsp-stream-url"
}
```

### 요청 예시

#### cURL
```bash
curl -X PATCH http://localhost:8080/cctvs/1/rtsp-url \
  -H "Content-Type: application/json" \
  -d '{
    "rtspUrl": "rtsp://100.101.102.103:8554/live"
  }'
```

#### HTTPie
```bash
http PATCH http://localhost:8080/cctvs/1/rtsp-url \
  rtspUrl="rtsp://100.101.102.103:8554/live"
```

#### JavaScript (Fetch API)
```javascript
fetch('http://localhost:8080/cctvs/1/rtsp-url', {
  method: 'PATCH',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    rtspUrl: 'rtsp://100.101.102.103:8554/live'
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

### 응답 예시

#### 성공 (200 OK)
```json
{
  "id": 1,
  "cctvName": "Building A CCTV",
  "rtspUrl": "rtsp://100.101.102.103:8554/stream",
  "location": {
    "id": 1,
    "fullAddress": "정보관 2층 01"
  },
  "createdAt": "2025-12-01T10:00:00",
  "updatedAt": "2025-12-02T15:30:00"
}
```

#### 실패 (404 Not Found)
```json
{
  "timestamp": "2025-12-02T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "CCTV not found: 999"
}
```

### 사용 시나리오
1. **휴대폰을 CCTV로 사용**: Tailscale을 통해 휴대폰 RTSP 스트림을 등록
2. **외부 CCTV 연동**: 기존 CCTV의 RTSP URL을 변경
3. **네트워크 변경**: IP 주소나 포트 변경 시 업데이트

### 참고사항
- RTSP URL은 `rtsp://` 프로토콜로 시작해야 합니다
- 업데이트 후 해당 CCTV로 라이브 스트리밍을 시작할 수 있습니다
- CCTV의 다른 정보(이름, 위치 등)는 변경되지 않습니다

**관련 코드**: `CCTVController.java`

---

## 4. Live-streaming 관련 API 사용법

### 개요
RTSP 라이브 스트리밍을 제어하고 상태를 조회하는 API입니다. FastAPI AI 서버와 통신하여 실시간 생존자 탐지 및 HLS 스트리밍 변환을 수행합니다.

### 4.1. 라이브 스트리밍 시작

#### 엔드포인트
```
POST /live-stream/start
```

#### Request Body
```json
{
  "cctvId": 1,
  "locationId": 1,
  "confThreshold": 0.5,         // (선택) 객체 탐지 신뢰도 임계값
  "poseConfThreshold": 0.5      // (선택) 포즈 추정 신뢰도 임계값
}
```

#### 응답 예시
**성공 (201 Created)**
```json
{
  "status": "success",
  "message": "라이브 스트리밍이 시작되었습니다",
  "cctvId": 1,
  "hlsUrl": "http://your-server:8080/streams/cctv1/playlist.m3u8",
  "rtspUrl": "rtsp://100.101.102.103:8554/live"
}
```

**실패 (500 Internal Server Error)**
```json
{
  "status": "error",
  "message": "라이브 스트리밍 시작에 실패했습니다: Connection refused",
  "cctvId": 1
}
```

#### cURL 예시
```bash
curl -X POST http://localhost:8080/live-stream/start \
  -H "Content-Type: application/json" \
  -d '{
    "cctvId": 1,
    "locationId": 1,
    "confThreshold": 0.6,
    "poseConfThreshold": 0.5
  }'
```

### 4.2. 라이브 스트리밍 중지

#### 엔드포인트
```
POST /live-stream/stop/{cctvId}
```

#### Path Variable
- `cctvId` (Long): 중지할 CCTV의 ID

#### 응답 예시
**성공 (200 OK)**
```json
{
  "status": "success",
  "message": "라이브 스트리밍이 중지되었습니다",
  "cctvId": 1
}
```

#### cURL 예시
```bash
curl -X POST http://localhost:8080/live-stream/stop/1
```

### 4.3. 라이브 스트리밍 상태 조회

#### 엔드포인트
```
GET /live-stream/status/{cctvId}
```

#### Path Variable
- `cctvId` (Long): 조회할 CCTV의 ID

#### 응답 예시
**스트리밍 진행 중**
```json
{
  "cctvId": 1,
  "isStreaming": true,
  "rtspUrl": "rtsp://100.101.102.103:8554/live",
  "hlsUrl": "http://your-server:8080/streams/cctv1/playlist.m3u8",
  "startedAt": "2025-12-02T15:00:00",
  "frameCount": 1523,
  "cctvName": "Building A CCTV",
  "location": "서울시 강남구 테헤란로 123"
}
```

**스트리밍 중지 상태**
```json
{
  "cctvId": 1,
  "isStreaming": false,
  "cctvName": "Building A CCTV",
  "location": "서울시 강남구 테헤란로 123"
}
```

#### cURL 예시
```bash
curl http://localhost:8080/live-stream/status/1
```

### 4.4. HLS URL 조회

#### 엔드포인트
```
GET /live-stream/url/{cctvId}
```

#### Path Variable
- `cctvId` (Long): HLS URL을 조회할 CCTV의 ID

#### 응답 예시
**성공 (200 OK)**
```
http://your-server:8080/streams/cctv1/playlist.m3u8
```

**실패 (404 Not Found)**
```
Live stream is not active for CCTV ID: 1
```

#### cURL 예시
```bash
curl http://localhost:8080/live-stream/url/1
```

### 사용 흐름
1. **CCTV RTSP URL 등록**: `PATCH /cctvs/{id}/rtsp-url`
2. **스트리밍 시작**: `POST /live-stream/start`
3. **HLS URL 획득**: 응답의 `hlsUrl` 필드 또는 `GET /live-stream/url/{cctvId}`
4. **비디오 플레이어에서 재생**: HLS.js, Video.js 등 사용
5. **실시간 탐지 정보 수신**: WebSocket 연결 (후술)
6. **스트리밍 중지**: `POST /live-stream/stop/{cctvId}`

### HLS 스트리밍 재생 예시

#### HTML5 Video (Safari)
```html
<video controls>
  <source src="http://your-server:8080/streams/cctv1/playlist.m3u8" type="application/x-mpegURL">
</video>
```

#### HLS.js (Chrome, Firefox 등)
```html
<video id="video" controls></video>
<script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
<script>
  const video = document.getElementById('video');
  const hlsUrl = 'http://your-server:8080/streams/cctv1/playlist.m3u8';

  if (Hls.isSupported()) {
    const hls = new Hls();
    hls.loadSource(hlsUrl);
    hls.attachMedia(video);
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = hlsUrl;
  }
</script>
```

**관련 코드**:
- `LiveStreamController.java`
- `LiveStreamServiceImpl.java`

---

## 5. Swagger 문서 주소와 사용법

### 접속 주소

#### 로컬 환경
```
http://localhost:8080/swagger-ui.html
```

#### 프로덕션 환경
```
http://your-server-url:8080/swagger-ui.html
```

### Swagger UI 기능

Swagger UI는 All-Clear 프로젝트의 모든 REST API를 대화형으로 테스트할 수 있는 웹 인터페이스를 제공합니다.

### API 그룹 구성

Swagger는 다음과 같이 API를 그룹화하여 제공합니다:

1. **all-apis**: 모든 API 엔드포인트 (WebSocket 제외)
2. **1. Member API** (`/members/**`): 회원 관리
3. **2. Survivor API** (`/survivors/**`): 생존자 정보 관리
4. **3. Location API** (`/locations/**`): 위치 정보 관리
5. **4. Detection API** (`/detections/**`): 탐지 기록 조회
6. **5. CCTV API** (`/cctvs/**`): CCTV 관리 및 RTSP URL 업데이트
7. **6. WiFi Sensor API** (`/wifi-sensors/**`): WiFi 센서 데이터
8. **7. Priority Assessment API** (`/priority-assessments/**`): 위험도 평가
9. **8. WebSocketTest API** (`/websocket/test/**`): WebSocket 테스트
10. **9. Live-Stream API** (`/live-stream/**`): 라이브 스트리밍 제어

### Swagger UI 사용법

#### 1. API 탐색
- 왼쪽 드롭다운에서 API 그룹을 선택합니다
- 각 엔드포인트를 클릭하여 상세 정보를 확인합니다

#### 2. API 테스트
1. 엔드포인트 클릭 → **Try it out** 버튼 클릭
2. 필요한 파라미터 입력
3. **Execute** 버튼 클릭
4. 응답 결과 확인 (Response Body, Headers, Status Code)

#### 3. 예시: RTSP URL 업데이트 테스트

**단계:**
1. 그룹 선택: `5. CCTV API`
2. 엔드포인트 선택: `PATCH /cctvs/{id}/rtsp-url`
3. **Try it out** 클릭
4. 파라미터 입력:
   - `id`: `1`
   - Request body:
     ```json
     {
       "rtspUrl": "rtsp://100.101.102.103:8554/live"
     }
     ```
5. **Execute** 클릭
6. 응답 확인

#### 4. 예시: 라이브 스트리밍 시작 테스트

**단계:**
1. 그룹 선택: `9. Live-Stream API`
2. 엔드포인트 선택: `POST /live-stream/start`
3. **Try it out** 클릭
4. Request body 입력:
   ```json
   {
     "cctvId": 1,
     "locationId": 1
   }
   ```
5. **Execute** 클릭
6. 응답에서 `hlsUrl` 확인
---

## 6. 실시간 스트리밍 진행시 생존자 탐지 및 위험도 점수 업데이트 과정

### 전체 흐름 다이어그램

```
[RTSP 카메라]
    ↓ RTSP 스트림
[FastAPI AI 서버]
    ↓ AI 분석 결과 (HTTP POST)
[Spring Boot 백엔드]
    ├─ 생존자 매칭/생성
    ├─ Detection 기록 저장
    ├─ 위험도 점수 계산
    └─ WebSocket 브로드캐스트
        ↓
[프론트엔드 클라이언트]
```

### 상세 프로세스

#### Phase 1: 라이브 스트리밍 시작

**1. 클라이언트 요청**
```
POST /live-stream/start
{
  "cctvId": 1,
  "locationId": 1
}
```

**2. Spring Boot 처리** (`LiveStreamServiceImpl.java`)
- CCTV 정보 조회 (RTSP URL 확인)
- FastAPI에 스트리밍 시작 요청 전송:
  ```json
  POST {AI_API_BASE_URL}/start_live_stream
  {
    "rtsp_url": "rtsp://...",
    "cctv_id": 1,
    "location_id": 1,
    "conf_threshold": 0.5,
    "pose_conf_threshold": 0.5
  }
  ```

**3. FastAPI AI 서버**
- RTSP 스트림 수신 시작
- 프레임별로 YOLO 모델 추론 수행
- HLS 세그먼트로 변환 (`.ts` 파일 + `playlist.m3u8` 생성)
- 분석 결과를 Spring Boot로 전송 (매 프레임 또는 주기적으로)

#### Phase 2: AI 탐지 결과 수신 및 처리

**1. FastAPI → Spring Boot**
```
POST /api/ai-detection-webhook
{
  "detections": [
    {
      "class_name": "human",
      "confidence": 0.87,
      "bbox": [100, 200, 150, 300],
      "pose": "sitting"
    },
    {
      "class_name": "fire",
      "confidence": 0.92,
      "bbox": [500, 400, 650, 550]
    }
  ],
  "summary": {
    "human_count": 1,
    "fire_count": 1,
    "smoke_count": 0,
    "total_objects": 2
  }
}
```

**2. AIDetectionProcessorService 처리** (`AIDetectionProcessorService.java`)

##### 2.1. 검증 및 엔티티 조회
```java
// CCTV와 Location 조회
CCTV cctv = cctvRepository.findById(cctvId).orElseThrow(...);
Location location = locationRepository.findById(locationId).orElseThrow(...);

// Null 체크 및 유효성 검증
if (allDetections == null || allDetections.isEmpty()) {
    log.warn("No detections found in AI result. Skipping processing.");
    return;
}
```

##### 2.2. Human 객체만 필터링 및 처리
```java
Set<Long> matchedSurvivorIds = new HashSet<>();  // 중복 매칭 방지

for (DetectionObject detection : allDetections) {
    if ("human".equalsIgnoreCase(detection.getClassName())) {
        processHumanDetection(detection, allDetections, summary,
                              cctv, location, videoUrl, matchedSurvivorIds);
    }
}
```

#### Phase 3: 생존자 매칭 또는 생성

**SurvivorMatchingService** (`AIDetectionProcessorService.java`)

##### 3.1. 기존 생존자 찾기 (바운딩박스 유사도 기반)
```java
Survivor survivor = survivorMatchingService.findOrCreateSurvivor(
    humanDetection,
    location,
    cctv,
    now,
    matchedSurvivorIds  // 이미 매칭된 생존자 제외
);
```

**매칭 알고리즘**:
- 같은 CCTV에서 마지막으로 탐지된 생존자들을 조회
- 바운딩박스 IoU(Intersection over Union) 계산
- IoU > 임계값(예: 0.3)이면 동일 생존자로 판단
- 매칭되지 않으면 새로운 생존자 생성

##### 3.2. 생존자 정보 업데이트
```java
if (!isNewSurvivor) {
    // 기존 생존자 업데이트
    survivor.setCurrentStatus(mapPoseToStatus(humanDetection.getPose()));
    survivor.setLastDetectedAt(now);
    survivorRepository.save(survivor);

    // WebSocket으로 업데이트 브로드캐스트
    webSocketService.broadcastSurvivorUpdate(survivor.getId(),
                                             SurvivorResponseDto.from(survivor));
} else {
    // 새 생존자 저장
    survivor = survivorRepository.save(survivor);

    // WebSocket으로 새 생존자 추가 브로드캐스트
    webSocketService.broadcastNewSurvivorAdded(
                                             SurvivorResponseDto.from(survivor));
}

matchedSurvivorIds.add(survivor.getId());  // 매칭 기록
```

**포즈 → 상태 매핑**:
- `sitting` → `sitting` (앉아 있음)
- `lying` → `lying` (쓰러져 있음)
- `standing` → `standing` (서 있음)
- `crawling` → `crawling` (기어가고 있음)

#### Phase 4: Detection 기록 생성

**Detection 엔티티 생성** (`AIDetectionProcessorService.java`)

```java
Detection detection = Detection.builder()
    .survivor(survivor)
    .detectionType(DetectionType.CCTV)
    .cctv(cctv)
    .location(location)
    .detectedAt(now)
    .detectedStatus(mapPoseToStatus(humanDetection.getPose()))
    .aiAnalysisResult(aiAnalysisJson)  // AI 분석 결과 JSON 저장
    .aiModelVersion("YOLO-ONNX-v1.0")
    .confidence(humanDetection.getConfidence())
    .videoUrl(videoUrl)
    .fireCount(summary.getFireCount())
    .humanCount(summary.getHumanCount())
    .smokeCount(summary.getSmokeCount())
    .totalObjects(summary.getTotalObjects())
    .build();

Detection savedDetection = detectionRepository.save(detection);

// WebSocket으로 실시간 브로드캐스트
webSocketService.broadcastDetectionUpdate(survivor.getId(),
                                          DetectionResponseDto.from(savedDetection));
```

**목적**:
- 시계열 추적: 생존자의 상태 변화 이력 기록
- AI 신뢰도: 각 탐지의 confidence 저장
- 환경 정보: 화재, 연기 등 주변 위험 요소 기록

#### Phase 5: 위험도 점수 계산 및 PriorityAssessment 생성

**PriorityService** (`AIDetectionProcessorService.java:150`)

```java
priorityService.createAssessmentFromAI(humanDetection,
                                       allDetections,
                                       summary,
                                       survivor,
                                       detection);
```

**위험도 점수 계산 요소** (논문 기반):

1. **환경 승수** 
   - 화재 감지: 생존자에게 직접 화재(*3.0), 공간 전체로 확산(*1.5), 단순 화재 감지(*1.0), 화재 거의 없음(*0.5), 화재 미감지(*0.1)
   - 연기 감지: 짙은 연기(*2.0)
   - 화재와 생존자 거리: 바운딩박스 IoU 기반

2. **생존자 상태 점수**
   - `Falling`: 10점
   - `Crawling`: 8점
   - `Sitting`: 5점
   - `Standing`: 3점

3. **최종 위험도 점수**
   - 생존자 상태점수 * 환경 승수

**PriorityAssessment 저장**:
```java
PriorityAssessment assessment = PriorityAssessment.builder()
    .survivor(survivor)
    .detection(detection)
    .priorityScore(calculatedScore)
    .environmentalRisk(envRisk)
    .physicalStatus(physicalStatus)
    .locationRisk(locationRisk)
    .assessedAt(now)
    .build();

priorityAssessmentRepository.save(assessment);

// WebSocket으로 브로드캐스트
webSocketService.broadcastPriorityUpdate(survivor.getId(),
                                         PriorityAssessmentResponseDto.from(assessment));
```

#### Phase 6: 실시간 알림 (WebSocket)

**WebSocketService** (`WebSocketServiceImpl.java`)

**구독 토픽**:
- `/topic/survivors/{survivorId}`: 특정 생존자 업데이트
- `/topic/detections/{survivorId}`: 탐지 정보 업데이트
- `/topic/priority/{survivorId}`: 위험도 점수 업데이트
- `/topic/survivors`: 전체 생존자 목록 업데이트

**프론트엔드 WebSocket 연결 예시**:
```javascript
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    onConnect: () => {
        // 생존자 업데이트 구독
        stompClient.subscribe('/topic/survivors/1', (message) => {
            const survivor = JSON.parse(message.body);
            console.log('Survivor updated:', survivor);
            updateUI(survivor);
        });

        // 위험도 점수 업데이트 구독
        stompClient.subscribe('/topic/priority/1', (message) => {
            const priority = JSON.parse(message.body);
            console.log('Priority updated:', priority);
            updatePriorityUI(priority);
        });
    }
});

stompClient.activate();
```

### 전체 데이터 흐름 요약

```
1. 라이브 스트리밍 시작
   → FastAPI가 RTSP 스트림 처리 시작

2. AI 모델 추론 (매 프레임)
   → 사람, 화재, 연기 탐지

3. Spring Boot로 탐지 결과 전송
   → AIDetectionProcessorService.processAIDetectionResult()

4. 생존자 매칭/생성
   → SurvivorMatchingService.findOrCreateSurvivor()
   → 바운딩박스 유사도 기반 동일인 판단

5. Detection 기록 생성
   → detectionRepository.save()
   → 시계열 추적 데이터 저장

6. 위험도 점수 계산
   → PriorityService.createAssessmentFromAI()
   → 환경, 상태, 신뢰도, 시간 요소 종합

7. 실시간 알림
   → WebSocketService.broadcastSurvivorUpdate()
   → WebSocketService.broadcastPriorityUpdate()
   → 프론트엔드에 즉시 반영
```

### 중요 특징

**1. 중복 매칭 방지**
- 같은 프레임에서 한 생존자가 여러 번 매칭되지 않도록 `matchedSurvivorIds` 사용

**2. 시계열 추적**
- Detection은 매 프레임마다 새로 생성 (히스토리 추적)
- Survivor는 재사용 (같은 사람)

**3. 실시간 업데이트**
- WebSocket을 통해 모든 변경사항을 즉시 브로드캐스트
- 프론트엔드는 폴링 없이 실시간 업데이트 수신

**4. 환경 기반 위험도**
- 단순히 사람 탐지뿐만 아니라 주변 화재, 연기 등을 고려
- 바운딩박스 겹침(IoU)으로 생존자와 위험 요소의 근접도 측정

**관련 코드**:
- `AIDetectionProcessorService.java`
- `LiveStreamServiceImpl.java`
- `SurvivorMatchingService.java`
- `PriorityServiceImpl.java`
- `WebSocketService.java` 및 `WebSocketServiceImpl.java`

---

## 7. WiFi CSI 센서 기반 생존자 탐지 시스템

### 개요

WiFi CSI(Channel State Information)를 이용해 
생존자 탐지(ESP32 WiFi 모듈이 벽이나 장애물 너머의 움직임 감지) 및 실시간으로 서버에 전송

### 시스템 아키텍처

```
[ESP32 WiFi 센서]
    ↓ WiFi CSI 신호 수집 (5초 간격)
    ↓ AI 모델 분석 (움직임 등 감지)
[MQTT 브로커]
    ↓ MQTT 메시지 발행
[Spring Boot 백엔드]
    ├─ WifiDetectionMqttService: MQTT 메시지 수신 및 처리
    ├─ WifiDetectionProcessorService: 생존자 매칭 및 DB 저장
    └─ WebSocketService: 실시간 브로드캐스트
        ↓
[프론트엔드 클라이언트]
    ├─ 실시간 CSI 신호 그래프
    ├─ 생존자 탐지 알림
    └─ 생존자 정보 업데이트
```

### WebSocket 구독 방법

#### 구독 토픽

WiFi 센서의 실시간 신호 데이터를 받으려면 다음 토픽을 구독하세요:

```
/topic/wifi-sensor/{sensorId}/signal
```

- `{sensorId}`: WiFi 센서 ID (예: `ESP32-001`, `ESP32-AABBCCDDEE`)
- 발행 주기: **5초마다** (생존자 탐지 여부와 무관하게 항상 발행됨)
- 데이터 형식: `WifiSignalDto` (JSON)

#### 프론트엔드 연결 예시

##### STOMP.js를 사용한 연결

```javascript
import { Client } from '@stomp/stompjs';

// WebSocket 클라이언트 생성
const stompClient = new Client({
    brokerURL: 'ws://localhost:8080/ws',
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    onConnect: (frame) => {
        console.log('WebSocket 연결 성공:', frame);

        // WiFi 센서 신호 구독 (센서 ID: ESP32-001)
        stompClient.subscribe('/topic/wifi-sensor/ESP32-001/signal', (message) => {
            const signalData = JSON.parse(message.body);
            console.log('WiFi 신호 수신:', signalData);

            // 그래프 업데이트
            updateSignalGraph(signalData);

            // 생존자가 탐지된 경우 알림 표시
            if (signalData.survivor_detected) {
                showSurvivorAlert(signalData);
                highlightSurvivor(signalData.survivor_id);
            }
        });
    },

    onStompError: (frame) => {
        console.error('STOMP 오류:', frame.headers['message']);
    }
});

// WebSocket 연결 시작
stompClient.activate();

// 그래프 업데이트 함수
function updateSignalGraph(signalData) {
    // 신호 강도를 실시간 그래프에 추가
    const chartData = {
        timestamp: signalData.timestamp,
        signalStrength: signalData.signal_strength,
        csiAmplitude: signalData.csi_amplitude_summary,
        movementIntensity: signalData.movement_intensity,
        breathingRate: signalData.breathing_rate
    };

    // Chart.js, D3.js 등을 사용하여 그래프 업데이트
    addChartDataPoint(chartData);
}

// 생존자 탐지 알림 함수
function showSurvivorAlert(signalData) {
    alert(`⚠️ 생존자 탐지됨!\n` +
          `센서: ${signalData.sensor_name}\n` +
          `위치: ${signalData.location_address}\n` +
          `생존자 번호: ${signalData.survivor_number}\n` +
          `신뢰도: ${(signalData.confidence * 100).toFixed(1)}%\n` +
          `신호 강도: ${signalData.signal_strength} dBm`);
}
```

##### SockJS를 사용한 연결 (브라우저 호환성 향상)

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    console.log('WebSocket 연결:', frame);

    // WiFi 센서 구독
    stompClient.subscribe('/topic/wifi-sensor/ESP32-001/signal', (message) => {
        const data = JSON.parse(message.body);
        handleWifiSignal(data);
    });
});
```

### WiFi 신호 데이터 형식 (WifiSignalDto)

WebSocket으로 수신되는 데이터 구조:

```json
{
  "sensor_id": "ESP32-001",
  "sensor_name": "1층 로비 센서",
  "location_id": 10,
  "location_address": "정보관 2층 01",
  "survivor_detected": true,
  "survivor_id": 42,
  "survivor_number": "S-042",
  "signal_strength": -45,
  "confidence": 0.87,
  "timestamp": "2025-12-03T14:30:25",
  "csi_amplitude_summary": [12.5, 15.3, 18.7, 14.2, 16.8],
  "movement_detected": true,
  "movement_intensity": 0.65,
  "breathing_detected": true,
  "breathing_rate": 18.5,
  "sensor_status": "ACTIVE",
  "battery_level": 85,
  "detailed_csi_analysis": {
    "csi_amplitude": [[12.5, 12.7, 12.9], [15.3, 15.5, 15.7]],
    "csi_phase": [[1.2, 1.3, 1.4], [2.1, 2.2, 2.3]],
    "movement_detected": true,
    "movement_intensity": 0.65,
    "breathing_detected": true,
    "breathing_rate": 18.5,
    "timestamp": "2025-12-03T14:30:25"
  }
}
```

#### 주요 필드 설명

**센서 정보**
- `sensor_id`: WiFi 센서 ID (ESP32 모듈 식별자)
- `sensor_name`: 센서 이름 (사용자 친화적)
- `location_id`: 위치 ID
- `location_address`: 위치 주소
- `sensor_status`: 센서 상태 (`ACTIVE`, `LOW_BATTERY`, `ERROR`)
- `battery_level`: 배터리 잔량 (0~100%)

**그래프 데이터**
- `signal_strength`: 신호 강도 (RSSI, dBm 단위) - **그래프 Y축**
- `timestamp`: 측정 시각 - **그래프 X축**
- `csi_amplitude_summary`: CSI 진폭 요약 (각 부반송파의 최신 값)
- `movement_intensity`: 움직임 강도 (0.0 ~ 1.0)
- `breathing_rate`: 호흡률 (분당 호흡 횟수, BPM)

**생존자 탐지 정보**
- `survivor_detected`: 생존자 탐지 여부 (`true` / `false`)
- `survivor_id`: 생존자 ID (탐지된 경우에만)
- `survivor_number`: 생존자 번호 (예: `S-001`, `S-042`)
- `confidence`: AI 모델 신뢰도 (0.0 ~ 1.0)

**움직임 및 호흡 감지**
- `movement_detected`: 움직임 감지 여부
- `movement_intensity`: 움직임 강도 (0.0 ~ 1.0)
- `breathing_detected`: 호흡 감지 여부
- `breathing_rate`: 호흡률 (분당 호흡 횟수)

**상세 분석 데이터** (생존자 탐지 시에만 포함)
- `detailed_csi_analysis`: 전체 CSI 분석 데이터 (상세 그래프용)
  - `csi_amplitude`: 각 부반송파별 CSI 진폭 시계열 데이터
  - `csi_phase`: 각 부반송파별 CSI 위상 시계열 데이터

### WiFi 신호 전달 프로세스

#### Phase 1: ESP32 센서 데이터 수집 및 발행

**1. WiFi CSI 신호 수집** (ESP32 모듈)
- ESP32 모듈이 5초마다 WiFi CSI 신호를 수집함
- AI 모델이 CSI 데이터를 분석하여 움직임 등 감지함
- 생존자 존재 여부를 판단함 (`survivor_detected`: true/false)

**2. MQTT 메시지 발행**
```json
MQTT Topic: /wifi-sensors/{locationId}/detection
Payload:
{
  "sensor_id": "ESP32-001",
  "location_id": 10,
  "survivor_detected": true,
  "signal_strength": -45,
  "confidence": 0.87,
  "timestamp": "2025-12-03T14:30:25",
  "csi_analysis": {
    "csi_amplitude": [[...]],
    "movement_detected": true,
    "breathing_detected": true,
    "breathing_rate": 18.5
  },
  "battery_level": 85,
  "status_message": "ACTIVE"
}
```

#### Phase 2: Spring Boot 백엔드 처리

**1. MQTT 메시지 수신** (`WifiDetectionMqttService.java`)

```java
@Transactional
public void processMqttMessage(MqttWifiDetectionDto mqttData) {
    // 1. 데이터 유효성 검증
    validateMqttData(mqttData);

    // 2. WiFi 센서 정보 조회
    WifiSensor sensor = wifiSensorRepository.findBySensorCode(mqttData.getSensorId())
        .orElseThrow(...);

    // 3. 위치 정보 조회
    Location location = locationRepository.findById(mqttData.getLocationId())
        .orElseThrow(...);

    // 4. 센서 상태 업데이트 (마지막 활성 시각, 신호 강도)
    sensor.setLastActiveAt(mqttData.getTimestamp());
    sensor.setSignalStrength(mqttData.getSignalStrength());
    wifiSensorRepository.save(sensor);

    // 5. WebSocket 브로드캐스트용 DTO 생성
    WifiSignalDto signalDto = WifiSignalDto.fromMqttData(
        mqttData, sensor.getSensorCode(), location.getFullAddress()
    );

    // 6. [항상 수행] WebSocket으로 실시간 신호 브로드캐스트
    webSocketService.broadcastWifiSignal(mqttData.getSensorId(), signalDto);

    // 7. [생존자 탐지 시에만] DB 저장 및 생존자 매칭
    if (Boolean.TRUE.equals(mqttData.getSurvivorDetected())) {
        wifiDetectionProcessorService.processDetection(mqttData, sensor, location, signalDto);
    }
}
```

**핵심 특징**:
- **항상 WebSocket 브로드캐스트**: 생존자 탐지 여부와 무관하게 5초마다 실시간 그래프 데이터를 전송함
- **조건부 DB 저장**: 생존자가 탐지된 경우에만 DB에 저장하여 스토리지 효율성을 높임

**2. WebSocket 브로드캐스트** (`WebSocketServiceImpl.java`)

```java
@Override
public void broadcastWifiSignal(String sensorId, WifiSignalDto signalData) {
    String destination = "/topic/wifi-sensor/" + sensorId + "/signal";
    messagingTemplate.convertAndSend(destination, signalData);

    if (Boolean.TRUE.equals(signalData.getSurvivorDetected())) {
        log.info("⚠️ [생존자 탐지!] WiFi 신호 브로드캐스트 - 센서: {}, 생존자 ID: {}, 신뢰도: {}",
                 sensorId, signalData.getSurvivorId(), signalData.getConfidence());
    }
}
```

#### Phase 3: 생존자 탐지 시 추가 처리

**생존자 매칭 또는 생성** (`WifiDetectionProcessorService.java`)

```java
@Transactional
public void processDetection(MqttWifiDetectionDto mqttData,
                              WifiSensor sensor,
                              Location location,
                              WifiSignalDto signalDto) {
    // 1. 같은 위치의 최근 생존자를 찾거나 새로 생성함
    Survivor survivor = findOrCreateSurvivor(location, mqttData.getTimestamp());
    boolean isNewSurvivor = survivor.getId() == null;

    // 2. 생존자 정보 업데이트
    updateSurvivorInfo(survivor, location, mqttData.getTimestamp());

    // 3. 생존자 저장
    survivor = survivorRepository.save(survivor);

    // 4. WifiSignalDto에 생존자 정보 설정
    signalDto.setSurvivorInfo(survivor.getId(), formatSurvivorNumber(survivor.getSurvivorNumber()));

    // 5. WebSocket으로 생존자 업데이트 브로드캐스트
    if (isNewSurvivor) {
        webSocketService.broadcastNewSurvivorAdded(SurvivorResponseDto.from(survivor));
    } else {
        webSocketService.broadcastSurvivorUpdate(survivor.getId(), SurvivorResponseDto.from(survivor));
    }

    // 6. Detection 레코드 생성 및 저장
    Detection detection = createDetection(mqttData, survivor, sensor, location, mqttData.getTimestamp());
    Detection savedDetection = detectionRepository.save(detection);

    // 7. WebSocket으로 탐지 정보 브로드캐스트
    webSocketService.broadcastDetectionUpdate(survivor.getId(), DetectionResponseDto.from(savedDetection));
}
```

**생존자 매칭 알고리즘**:
- 같은 위치(`Location`)에서 최근 **10분 이내**에 WiFi로 탐지된 생존자를 조회함
- 매칭되면 기존 생존자 재사용 (마지막 탐지 시각만 업데이트)
- 매칭되지 않으면 새로운 생존자 생성
- 시간 임계값 변경: `WifiDetectionProcessorService.java:55` (`TIME_THRESHOLD_MINUTES`)

#### Phase 4: 프론트엔드 수신 및 렌더링

**1. WebSocket 메시지 수신**
- 프론트엔드는 `/topic/wifi-sensor/{sensorId}/signal` 토픽을 구독함
- 5초마다 `WifiSignalDto` 데이터를 수신함

**2. 그래프 업데이트**
- `signal_strength`, `csi_amplitude_summary`, `movement_intensity`, `breathing_rate` 등을 그래프에 추가함
- 시간축(`timestamp`)과 신호 강도 축(`signal_strength`)을 사용하여 실시간 그래프를 렌더링함

**3. 생존자 탐지 시 특수 효과**
- `survivor_detected: true`인 경우:
  - 알림 팝업 표시
  - 생존자 마커 추가
  - 그래프에 하이라이트 표시
  - `detailed_csi_analysis`를 사용하여 상세 그래프 렌더링

### 전체 데이터 흐름 요약

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. ESP32 WiFi 센서                                                      │
│    - 5초마다 WiFi CSI 신호 수집                                          │
│    - AI 모델 분석 (움직임/호흡 감지)                                      │
│    - 생존자 탐지 여부 판단                                                │
└────────────────────────────┬────────────────────────────────────────────┘
                             ↓ MQTT 발행
┌─────────────────────────────────────────────────────────────────────────┐
│ 2. Spring Boot 백엔드 (WifiDetectionMqttService)                        │
│    - MQTT 메시지 수신                                                    │
│    - 센서 및 위치 정보 조회                                               │
│    - 센서 상태 업데이트                                                   │
└────────────────────────────┬────────────────────────────────────────────┘
                             ↓
              ┌──────────────┴──────────────┐
              ↓ (항상)                      ↓ (생존자 탐지 시에만)
┌─────────────────────────────┐  ┌─────────────────────────────────────┐
│ 3. WebSocket 브로드캐스트    │  │ 4. 생존자 매칭 및 DB 저장            │
│    - /topic/wifi-sensor/    │  │    - 기존 생존자 찾기 또는 새로 생성  │
│      {sensorId}/signal      │  │    - Survivor 저장                  │
│    - 실시간 그래프 데이터     │  │    - Detection 레코드 생성          │
│      (5초마다)               │  │    - 생존자 정보 WebSocket 브로드캐스트│
└─────────────┬───────────────┘  └─────────────┬───────────────────────┘
              ↓                                 ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 5. 프론트엔드 클라이언트                                                  │
│    - 실시간 CSI 신호 그래프 렌더링                                         │
│    - 생존자 탐지 알림 표시                                                │
│    - 생존자 정보 업데이트                                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

### 주요 특징

**1. 실시간 모니터링**
- 5초마다 WiFi 신호 데이터를 프론트엔드에 전송함
- 생존자 탐지 여부와 무관하게 항상 그래프 데이터를 제공함
- WebSocket을 통한 양방향 통신으로 지연 시간 최소화

**2. 효율적인 데이터 관리**
- **평상시**: WebSocket 브로드캐스트만 수행 (DB 저장 안 함)
- **생존자 탐지 시**: DB 저장 + WebSocket 브로드캐스트
- 스토리지 사용량을 최소화하면서도 실시간 모니터링 가능

**3. 생존자 매칭 시스템**
- 같은 위치에서 최근 10분 이내 탐지된 생존자를 재사용함
- 중복 생존자 생성을 방지하고 추적 정확도를 높임
- 시간 임계값은 설정 변경 가능 (`WifiDetectionProcessorService.java:55`)

### 그래프 표현 필드 조정 방법

프론트엔드 그래프에 표시할 데이터를 변경하려면:

**1. `WifiSignalDto.java` 수정**
- 새로운 필드를 추가하거나 기존 필드를 수정함
- 예: 새로운 센서 데이터 추가, 필드명 변경 등

**2. `WifiSignalDto.fromMqttData()` 메서드 수정** (`WifiSignalDto.java:160-200`)
- MQTT 데이터를 WebSocket DTO로 변환하는 로직
- 새 필드를 추가했다면 이 메서드에서도 해당 필드를 설정해야 함

**현재 그래프용 주요 필드**:
- `signalStrength`: 신호 강도 (Y축)
- `timestamp`: 시간 (X축)
- `csiAmplitudeSummary`: CSI 진폭 요약
- `movementIntensity`: 움직임 강도
- `breathingRate`: 호흡률

### 생존자 매칭 시간 임계값 조정

**현재 설정**: 10분

**수정 위치**: `WifiDetectionProcessorService.java:55`

```java
private static final int TIME_THRESHOLD_MINUTES = 10;
```

이 값을 원하는 시간(분 단위)으로 변경하면 됩니다.

**예시**:
- 5분으로 변경: `private static final int TIME_THRESHOLD_MINUTES = 5;`
- 15분으로 변경: `private static final int TIME_THRESHOLD_MINUTES = 15;`

### MQTT 연동 설정

**환경 변수 설정**:
```bash
export MQTT_ENABLED=true  # MQTT 기능 활성화
```

**application.yml** (MQTT 브로커 설정):
```yaml
mqtt:
  enabled: ${MQTT_ENABLED:false}
```

### REST API 엔드포인트

프론트엔드에서 사용할 수 있는 WiFi 센서 관련 REST API입니다. 실시간 신호 데이터는 WebSocket을 사용하고, 센서 관리는 REST API를 사용합니다.

#### GET /wifi-sensors - WiFi 센서 목록 조회

모든 WiFi 센서 목록을 조회합니다. `active` 파라미터로 활성 센서만 필터링할 수 있습니다.

**요청 예시**
```bash
# 전체 센서 조회
GET /wifi-sensors

# 활성 센서만 조회
GET /wifi-sensors?active=true

# 비활성 센서만 조회
GET /wifi-sensors?active=false
```

**응답 예시**
```json
[
  {
    "id": 1,
    "sensorCode": "ESP32-001",
    "location": {
      "id": 10,
      "fullAddress": "정보관 2층 01"
    },
    "signalStrength": -45,
    "isActive": true,
    "lastActiveAt": "2025-12-03T14:30:25",
    "createdAt": "2025-12-01T10:00:00"
  },
  {
    "id": 2,
    "sensorCode": "ESP32-002",
    "location": {
      "id": 11,
      "fullAddress": "정보관 3층 02"
    },
    "signalStrength": -52,
    "isActive": true,
    "lastActiveAt": "2025-12-03T14:30:20",
    "createdAt": "2025-12-01T11:00:00"
  }
]
```

#### POST /wifi-sensors - WiFi 센서 등록

새로운 WiFi 센서를 시스템에 등록합니다.

**요청 예시**
```bash
POST /wifi-sensors
Content-Type: application/json

{
  "sensorCode": "ESP32-003",
  "locationId": 12
}
```

**응답 예시**
```json
{
  "id": 3,
  "sensorCode": "ESP32-003",
  "location": {
    "id": 12,
    "fullAddress": "정보관 4층 03"
  },
  "signalStrength": null,
  "isActive": false,
  "lastActiveAt": null,
  "createdAt": "2025-12-03T14:35:00"
}
```

#### DELETE /wifi-sensors/{id} - WiFi 센서 삭제

WiFi 센서를 시스템에서 제거합니다.

**요청 예시**
```bash
DELETE /wifi-sensors/3
```

**응답 예시**
```
WiFi_Sensor deleted successfully
```

#### 사용 시나리오

**1. 센서 목록 표시**
```javascript
// 활성 센서만 가져와서 표시
fetch('/wifi-sensors?active=true')
  .then(response => response.json())
  .then(sensors => {
    sensors.forEach(sensor => {
      displaySensorOnMap(sensor);
      // 각 센서의 실시간 신호는 WebSocket으로 구독
      subscribeToSensor(sensor.sensorCode);
    });
  });
```

**2. 센서 등록 후 WebSocket 구독**
```javascript
// 센서 등록
const newSensor = await fetch('/wifi-sensors', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    sensorCode: 'ESP32-NEW',
    locationId: 15
  })
}).then(res => res.json());

// WebSocket 구독 시작
stompClient.subscribe(`/topic/wifi-sensor/${newSensor.sensorCode}/signal`, handleSignal);
```

### 관련 코드

- `WifiDetectionMqttService.java`: MQTT 메시지 수신 및 처리
- `WifiDetectionProcessorService.java`: 생존자 매칭 및 DB 저장
- `WifiSensorController.java`: REST API 엔드포인트
- `WifiSignalDto.java`: WebSocket 브로드캐스트용 DTO
- `MqttWifiDetectionDto.java`: MQTT 메시지 파싱 DTO
- `WifiAnalysisDataDto.java`: CSI 분석 데이터 DTO
- `WebSocketService.java` 및 `WebSocketServiceImpl.java`: WebSocket 브로드캐스트

---

### 기술 스택

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Build Tool**: Gradle 8.x
- **Database**: Oracle Database (Oracle RDS)
- **ORM**: Spring Data JPA, Hibernate
- **API Documentation**: SpringDoc OpenAPI 3 (Swagger)
- **Real-time Communication**: WebSocket (STOMP)
- **External Integration**:
  - FastAPI (AI 모델 서버)
  - RTSP (비디오 스트리밍)
  - HLS (HTTP Live Streaming)

### 관련 문서
- [Swagger UI](http://localhost:8080/swagger-ui.html): API 문서 (실행 후 접속)
