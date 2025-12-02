# All-Clear 프로젝트 백엔드 API

재난 상황에서 생존자를 탐지하고 위험도를 평가하는 AI 기반 실시간 모니터링 시스템

## 목차
1. [서버 실행 방법](#1-서버-실행-방법)
2. [파일 구조](#2-파일-구조)
3. [PATCH /cctvs/{id}/rtsp-url 사용법](#3-patch-cctvsidrtsp-url-사용법)
4. [Live-streaming 관련 API 사용법](#4-live-streaming-관련-api-사용법)
5. [Swagger 문서 주소와 사용법](#5-swagger-문서-주소와-사용법)
6. [실시간 스트리밍 진행시 생존자 탐지 및 탐지정보, 위험도 점수 업데이트되는 과정](#6-실시간-스트리밍-진행시-생존자-탐지-및-위험도-점수-업데이트-과정)
7. (추가 예정) [WiFi 신호 탐지 관련 API 사용법]
---

## 1. 서버 실행 방법

### 사전 준비

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

### 빌드 및 실행

#### 개발 환경
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build
```

#### 프로덕션 배포
```bash
# JAR 파일 생성
./gradlew bootJar

# JAR 파일 실행
java -jar build/libs/project-0.0.1-SNAPSHOT.jar
```

#### 자동 배포 (GitHub Actions)
`main` 브랜치에 푸시하면 자동으로 EC2 인스턴스에 배포됩니다.
- `.github/workflows/deploy.yml` 워크플로우가 실행됩니다
- 필요한 GitHub Secrets: `EC2_SSH_KEY`, `EC2_HOST`, `EC2_USER`

### 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 상세 출력과 함께 테스트
./gradlew test --info

# 특정 테스트 클래스만 실행
./gradlew test --tests "opensource.project.ProjectApplicationTests"
```

### 서버 확인
서버가 정상적으로 실행되면 다음 주소로 접근할 수 있습니다:
- 애플리케이션: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

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
│   │   │   │   └── WifiSensorController.java
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
│   │   │       ├── AIDetectionProcessorService.java  # AI 탐지 결과 처리
│   │   │       ├── CCTVService.java / CCTVServiceImpl.java
│   │   │       ├── LiveStreamService.java / LiveStreamServiceImpl.java
│   │   │       ├── PriorityService.java / PriorityServiceImpl.java
│   │   │       ├── SurvivorMatchingService.java      # 생존자 매칭 로직
│   │   │       ├── WebSocketService.java             # 실시간 알림
│   │   │       └── ... (기타 Service)
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
