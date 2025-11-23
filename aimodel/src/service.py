"""
===============================================================================
[수정 이력] service.py - 원본 대비 변경사항
===============================================================================
1. import 경로 변경 (line 9-11)
   - 원본: from .yolo_model import YOLOOnnx (상대 경로)
   - 수정: from yolo_model import YOLOOnnx (절대 경로)
   - 이유: EC2 배포 시 직접 실행 방식과 호환성 확보

2. 경로 설정 개선 (line 16-17, 21)
   - 원본: MODEL_PATH = "./model/best_human.onnx" (상대 경로)
   - 수정: BASE_DIR 기준 os.path.join() 사용 (절대 경로)
   - 이유: 작업 디렉토리와 관계없이 안정적인 경로 참조

3. POSE_CLASS_NAMES 변경 (line 22)
   - 원본: ["Crawling", "Falling", "Sitting", "Standing"] (원본도 4개였으나 주석은 3개로 설명)
   - 수정: 4개 클래스 명확히 정의 및 주석 수정
   - 이유: 새 모델의 4개 클래스 지원

4. 모델 로딩 안정성 개선 (line 38-68)
   - 원본: yolo = YOLOOnnx(MODEL_PATH, CLASS_NAMES) (검증 없음)
   - 수정: 모델 파일 존재 여부/크기 확인 로그 추가, try-except로 Pose 모델 로딩 실패 처리
   - 이유: 배포 시 문제 진단 용이, 모델 로딩 실패해도 서비스는 계속 실행

5. pose_model None 체크 추가 (line 219, 227, 297, 303, 503, 511)
   - 원본: pose_model.predict_pose() 직접 호출
   - 수정: if pose_model is not None: 조건 추가
   - 이유: Pose 모델 로딩 실패 시에도 서비스 정상 동작

6. Spring Boot 연동 설정 추가 (line 27-35)
   - 원본: 없음
   - 수정: SPRING_BOOT_URL, STREAM_OUTPUT_DIR, 신뢰도 임계값 설정 추가
   - 이유: 영상 분석 기능 지원

7. /analyze_video 엔드포인트 추가 (line 324-646)
   - 원본: 없음 (이미지 분석만 지원)
   - 수정: 동영상 분석 + HLS 변환 + Spring Boot API 호출 기능 추가
   - 이유: 실시간 CCTV 영상 분석 지원
=====================================================
"""
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse, FileResponse
import uvicorn
import cv2
import numpy as np
import os
from collections import Counter
# [수정] 원본: from .yolo_model import YOLOOnnx (상대 경로)
# EC2 배포 시 직접 실행 방식과 호환되도록 절대 경로로 변경
from yolo_model import YOLOOnnx
from yolo_pose_model import YOLOPoseOnnx

# ----------------- CONFIG -----------------
# [수정] 원본: MODEL_PATH = "./model/best_human.onnx" (상대 경로)
# 작업 디렉토리와 관계없이 안정적인 경로 참조를 위해 절대 경로로 변경
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(BASE_DIR, "model", "best_human.onnx")
CLASS_NAMES = ["fire", "human", "smoke"]

# --- POSE MODEL CONFIG ---
POSE_MODEL_PATH = os.path.join(BASE_DIR, "model", "best_pose.onnx")
# [수정] 원본: 3개 클래스 → 4개 클래스 (Crawling 추가, Fall → Falling 명칭 변경)
POSE_CLASS_NAMES = ["Crawling", "Falling", "Sitting", "Standing"]

SAVE_DIR = os.path.join(BASE_DIR, "predictions")
os.makedirs(SAVE_DIR, exist_ok=True)

# Spring Boot URL (같은 EC2 서버에서 실행)
SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")

# 스트림 URL 베이스 (프론트엔드에서 접근하는 주소, 포트 없이 EC2 퍼블릭 주소 사용)
STREAM_BASE_URL = os.getenv("STREAM_BASE_URL", "http://localhost:8080")

# HLS 스트림 출력 디렉토리 (영상 분석 결과를 HLS로 변환하여 저장)
STREAM_OUTPUT_DIR = "/home/ubuntu/streams"

# 신뢰도 임계값
DEFAULT_DETECTION_CONF_THRESHOLD = 0.5
DEFAULT_POSE_CONF_THRESHOLD = 0.3
# ===================================================================================

# ----------------- INIT -----------------
# ============ 수정 사항: 모델 로딩 안정성 개선 ============
# 1. 기본 객체 탐지 모델 (fire, human, smoke)
# 원본: yolo = YOLOOnnx(MODEL_PATH, CLASS_NAMES) (검증 없이 바로 로드)
# 추가: 모델 파일 존재 여부 및 크기 확인 로그
print(f"Loading YOLO model from: {MODEL_PATH}")
print(f"Model file exists: {os.path.exists(MODEL_PATH)}")
if os.path.exists(MODEL_PATH):
    print(f"Model file size: {os.path.getsize(MODEL_PATH)} bytes")
else:
    print(f"ERROR: Model file not found at {MODEL_PATH}")
    print(f"Directory contents: {os.listdir(os.path.dirname(MODEL_PATH)) if os.path.exists(os.path.dirname(MODEL_PATH)) else 'Directory not found'}")

yolo = YOLOOnnx(MODEL_PATH, CLASS_NAMES)

# 2. 자세 분류 모델 (Crawling, Falling, Sitting, Standing)
# YOLOPoseOnnx 클래스를 사용하여 초기화
# 분류 모델의 입력 크기에 맞게 input_shape를 (224, 224) 등으로 변경할 수 있음
print(f"Loading Pose model from: {POSE_MODEL_PATH}")
print(f"Pose model file exists: {os.path.exists(POSE_MODEL_PATH)}")

# 원본: pose_model = YOLOPoseOnnx(POSE_MODEL_PATH, POSE_CLASS_NAMES, input_shape=(640, 640))
# 추가: try-except로 Pose 모델 로딩 실패 시 로그 출력(서비스는 실행됨)
pose_model = None
try:
    pose_model = YOLOPoseOnnx(POSE_MODEL_PATH, POSE_CLASS_NAMES, input_shape=(640, 640))
    print("[SUCCESS] Pose model loaded successfully")
except Exception as e:
    print(f"[FAILED] WARNING: Failed to load pose model: {e}")
    print("Pose detection features will be disabled, but the service will continue")
# ======================================================== 

app = FastAPI(title="YOLO ONNX Detection API")

# ----------------- HELPER FUNCTION -----------------
def draw_pose_predictions(img, detections):
    """
    새로운 detections JSON 구조를 기반으로 모든 바운딩 박스와 레이블을 그립니다
    'human' 클래스에는 'pose' 정보를 추가로 표시
    """
    img_copy = img.copy()
    for det in detections:
        box = det.get("box")
        if box is None:
            continue
            
        x1, y1, x2, y2 = map(int, [box.get("x1", 0), box.get("y1", 0), box.get("x2", 0), box.get("y2", 0)])
        class_name = det.get("class", "Unknown")
        confidence = det.get("confidence", 0)
        
        # 클래스별 색상 설정
        if class_name == "fire":
            color = (0, 0, 255) # Red
        elif class_name == "smoke":
            color = (100, 100, 100) # Gray
        elif class_name == "human":
            color = (0, 255, 0) # Green
        else:
            color = (255, 0, 0) # Blue
            
        # 레이블 생성
        label = f"{class_name} ({confidence*100:.0f}%)"
        
        # 'human' 클래스일 경우, 'pose' 정보 추가
        if class_name == "human":
            pose = det.get('pose', 'Unknown')
            pose_score = det.get('pose_score', 0)
            label = f"Human: {pose} ({pose_score*100:.0f}%)" # 포즈 점수를 표시하도록 수정
            
        # 사각형 및 텍스트 그리기
        cv2.rectangle(img_copy, (x1, y1), (x2, y2), color, 2)
        # 텍스트 배경 추가 (가독성)
        (w, h), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)
        cv2.rectangle(img_copy, (x1, y1 - h - 10), (x1 + w, y1 - 10), color, -1)
        cv2.putText(img_copy, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        
    return img_copy

# ----------------- HEALTH -----------------
@app.get("/health")
def health():
    return {"status": "ok"}

# ----------------- STANDARD PREDICT (JSON) -----------------
@app.post("/predict")
async def predict(file: UploadFile = File(...), conf_threshold: float = 0.5):
    """
    기본 YOLO 모델을 실행하고, 원하시는 JSON 포맷으로 결과를 반환합니다.
    (yolo.predict가 이 포맷을 반환한다고 가정)
    """
    # 임시 파일 저장
    temp_path = os.path.join(SAVE_DIR, f"temp_base_{file.filename}")
    contents = await file.read()
    with open(temp_path, "wb") as f:
        f.write(contents)
    
    result_image_path = os.path.join(SAVE_DIR, f"pred_base_{file.filename}")
    result = yolo.predict(temp_path, conf_threshold=conf_threshold, save_path=result_image_path)
    
    if os.path.exists(temp_path):
        os.remove(temp_path)

    if "image_path" not in result:
        result["image_path"] = result_image_path

    return JSONResponse(content=result)

# ----------------- STANDARD PREDICT (IMAGE) -----------------
@app.post("/predict_image")
async def predict_image(file: UploadFile = File(...), conf_threshold: float = 0.5):
    """
    기본 YOLO 모델을 실행하고, 주석이 달린 이미지를 반환합니다.
    """
    temp_path = os.path.join(SAVE_DIR, f"temp_base_img_{file.filename}")
    contents = await file.read()
    with open(temp_path, "wb") as f:
        f.write(contents)

    result_image_path = os.path.join(SAVE_DIR, f"pred_base_img_{file.filename}")
    result = yolo.predict(temp_path, conf_threshold=conf_threshold, save_path=result_image_path)
    
    if os.path.exists(temp_path):
        os.remove(temp_path)
    
    image_path = result.get("image_path")
    if not image_path or not os.path.exists(image_path):
        return JSONResponse(status_code=404, content={"error": "Annotated image not found"})
        
    return FileResponse(image_path, media_type="image/jpeg")

# ----------------- POSE PIPELINE (JSON) -----------------
@app.post("/predict_with_pose")
async def predict_with_pose(
    file: UploadFile = File(...), 
    conf_threshold: float = 0.5, 
    pose_conf_threshold: float = 0.3
):
    """
    YOLO (Detection) + YOLOPose (Classification) 파이프라인을 실행하고, 
    'pose'가 추가된 JSON을 반환합니다.
    """
    # 1. 이미지 읽기
    contents = await file.read()
    np_arr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    
    if img is None:
        return JSONResponse(status_code=400, content={"error": "Invalid image"})

    # 2. 임시 원본 파일 저장 (yolo.predict가 파일 경로를 받으므로)
    temp_path = os.path.join(SAVE_DIR, f"temp_pose_{file.filename}")
    cv2.imwrite(temp_path, img)

    # 3. 기본 YOLO 모델 실행 (주석/저장 없이 JSON만 받기)
    yolo_result = yolo.predict(temp_path, conf_threshold=conf_threshold, save_path=None) 
    
    if "detections" not in yolo_result:
        os.remove(temp_path)
        return JSONResponse(status_code=500, content={"error": "Invalid base prediction format", "data": yolo_result})

    # 4. 'human' 객체에 대해 Pose 모델 실행
    final_detections = []
    pose_counts = Counter()

    for det in yolo_result.get("detections", []):
        if det.get("class") == "human":
            # 4a. 'human' 박스 좌표로 원본 이미지 크롭
            box = det.get("box", {})
            x1, y1, x2, y2 = map(int, [box.get("x1"), box.get("y1"), box.get("x2"), box.get("y2")])
            
            # 크롭 (패딩 없이 원본 박스 사용, 필요시 패딩 추가)
            cropped_img = img[y1:y2, x1:x2]
            
            if cropped_img.size == 0:
                det["pose"] = "Unknown (Crop Error)"
                det["pose_score"] = 0.0
                final_detections.append(det)
                continue

            # 4c. Pose 모델 실행 (새로운 YOLOPoseOnnx 클래스 사용)
            # predict_pose는 (class_name, score)를 반환
            if pose_model is not None:
                detected_pose, pose_score = pose_model.predict_pose(cropped_img, pose_conf_threshold)

                # 4e. 'pose' 정보 추가
                det["pose"] = detected_pose
                det["pose_score"] = pose_score # pose 점수도 추가
                pose_counts[detected_pose] += 1
            else:
                det["pose"] = "Unknown (Model Not Loaded)"
                det["pose_score"] = 0.0
            
        final_detections.append(det)

    # 5. 최종 Summary 업데이트
    base_summary = yolo_result.get("summary", {})
    base_summary["pose_counts"] = dict(pose_counts) # 예: {"Falling": 1, "Standing": 2}

    # 6. 주석이 달린 최종 이미지 생성 및 저장 (JSON에 경로 포함)
    annotated_img = draw_pose_predictions(img, final_detections)
    final_image_path = os.path.join(SAVE_DIR, f"final_pose_{file.filename}")
    cv2.imwrite(final_image_path, annotated_img)
    
    # 7. 임시 원본 파일 삭제
    os.remove(temp_path)

    # 8. 최종 JSON 반환
    return JSONResponse(content={
        "image_path": final_image_path,
        "detections": final_detections,
        "summary": base_summary
    })

# ----------------- POSE PIPELINE (IMAGE) -----------------
@app.post("/predict_image_with_pose")
async def predict_image_with_pose(
    file: UploadFile = File(...), 
    conf_threshold: float = 0.5, 
    pose_conf_threshold: float = 0.3
):
    """
    YOLO + Pose 파이프라인을 실행하고, 'pose' 정보까지 시각화된
    최종 이미지를 반환합니다.
    """
    # 1. 이미지 읽기
    contents = await file.read()
    np_arr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    
    if img is None:
        return JSONResponse(status_code=400, content={"error": "Invalid image"})

    # 2. 임시 원본 파일 저장
    temp_path = os.path.join(SAVE_DIR, f"temp_pose_img_{file.filename}")
    cv2.imwrite(temp_path, img)

    # 3. 기본 YOLO 모델 실행
    yolo_result = yolo.predict(temp_path, conf_threshold=conf_threshold, save_path=None) 
    
    if "detections" not in yolo_result:
        os.remove(temp_path)
        return JSONResponse(status_code=500, content={"error": "Invalid base prediction format", "data": yolo_result})

    # 4. 'human' 객체에 대해 Pose 모델 실행
    final_detections = []
    
    for det in yolo_result.get("detections", []):
        if det.get("class") == "human":
            box = det.get("box", {})
            x1, y1, x2, y2 = map(int, [box.get("x1"), box.get("y1"), box.get("x2"), box.get("y2")])
            
            cropped_img = img[y1:y2, x1:x2]
            
            if cropped_img.size == 0:
                det["pose"] = "Unknown"
                final_detections.append(det)
                continue

            # 4c. Pose 모델 실행 (새로운 클래스 사용)
            if pose_model is not None:
                detected_pose, pose_score = pose_model.predict_pose(cropped_img, pose_conf_threshold)

                det["pose"] = detected_pose
                det["pose_score"] = pose_score # 이미지 레이블에 점수를 쓰기 위해
            else:
                det["pose"] = "Unknown (Model Not Loaded)"
                det["pose_score"] = 0.0
            
        final_detections.append(det)

    # 5. 주석이 달린 최종 이미지 생성 및 저장
    annotated_img = draw_pose_predictions(img, final_detections)
    final_image_path = os.path.join(SAVE_DIR, f"final_pose_image_{file.filename}")
    cv2.imwrite(final_image_path, annotated_img)
    
    # 6. 임시 원본 파일 삭제
    os.remove(temp_path)

    # 7. 최종 이미지 반환
    return FileResponse(final_image_path, media_type="image/jpeg")

# ============ 추가 기능: VIDEO ANALYSIS + HLS STREAMING ============
# 새로운 기능
# 동영상 파일을 분석하여 YOLO 탐지 + Pose 분류를 수행하고,
# 결과를 Spring Boot API로 전송하며, HLS 스트리밍 파일을 생성
# ====================================================================
@app.post("/analyze_video")
async def analyze_video(
    video_path: str,
    cctv_id: int,
    location_id: int,
    spring_boot_url: str = SPRING_BOOT_URL,
    output_stream_dir: str = STREAM_OUTPUT_DIR,
    conf_threshold: float = DEFAULT_DETECTION_CONF_THRESHOLD,
    pose_conf_threshold: float = DEFAULT_POSE_CONF_THRESHOLD
):
    """
    동영상 분석 + HLS 변환 + Spring Boot API 호출

    Args:
        video_path: 분석할 동영상 파일 경로
        cctv_id: CCTV ID
        location_id: 위치 ID
        spring_boot_url: Spring Boot 서버 URL
        output_stream_dir: HLS 출력 디렉토리
        conf_threshold: 객체 탐지 신뢰도 임계값
        pose_conf_threshold: 자세 분류 신뢰도 임계값
    """
    import subprocess
    import requests
    import threading
    import traceback
    import gc

    # 백그라운드에서 비동기로 분석 실행
    def run_analysis_in_background():
        try:
            analyze_video_sync(video_path, cctv_id, location_id, spring_boot_url,
                             output_stream_dir, conf_threshold, pose_conf_threshold)
        except Exception as e:
            print(f"Background analysis error: {str(e)}")
            print(traceback.format_exc())

    # 백그라운드 스레드 시작
    analysis_thread = threading.Thread(target=run_analysis_in_background)
    analysis_thread.daemon = True
    analysis_thread.start()

    # 즉시 응답 반환 (분석은 백그라운드에서 계속 진행)
    return JSONResponse(content={
        "status": "processing",
        "message": "Video analysis started in background",
        "cctv_id": cctv_id,
        "location_id": location_id,
        "video_path": video_path
    })

def analyze_video_sync(
    video_path: str,
    cctv_id: int,
    location_id: int,
    spring_boot_url: str,
    output_stream_dir: str,
    conf_threshold: float,
    pose_conf_threshold: float
):
    """
    실제 비디오 분석 로직 (동기 실행) - 라이브 스트리밍 방식
    분석하면서 동시에 FFmpeg로 HLS 세그먼트를 실시간 생성
    """
    import subprocess
    import requests
    import traceback
    import gc

    try:
        # 경로 검증 (로그 확인용)
        print(f"=== Video Analysis Request (Live Streaming Mode) ===")
        print(f"Video path: {video_path}")
        print(f"CCTV ID: {cctv_id}, Location ID: {location_id}")

        if not os.path.exists(video_path):
            error_msg = f"Video file not found: {video_path}"
            print(f"ERROR: {error_msg}")
            return JSONResponse(status_code=404, content={"error": error_msg})

        if not os.path.isfile(video_path):
            error_msg = f"Path is not a file: {video_path}"
            print(f"ERROR: {error_msg}")
            return JSONResponse(status_code=400, content={"error": error_msg})

        # 파일 읽기 권한 확인
        if not os.access(video_path, os.R_OK):
            error_msg = f"No read permission for file: {video_path}"
            print(f"ERROR: {error_msg}")
            return JSONResponse(status_code=403, content={"error": error_msg})

        print(f"Video file validated successfully")

    except Exception as e:
        error_msg = f"Video path validation failed: {str(e)}"
        print(f"ERROR: {error_msg}")
        print(traceback.format_exc())
        return JSONResponse(status_code=500, content={"error": error_msg, "traceback": traceback.format_exc()})

    # HLS 출력 디렉토리 설정
    stream_output_dir_path = os.path.join(output_stream_dir, f"cctv{cctv_id}")
    os.makedirs(stream_output_dir_path, exist_ok=True)
    hls_output = os.path.join(stream_output_dir_path, "playlist.m3u8")

    # 기존 HLS 파일 삭제 (새로운 스트림을 위해)
    import glob
    for old_file in glob.glob(os.path.join(stream_output_dir_path, "*.ts")):
        os.remove(old_file)
    for old_file in glob.glob(os.path.join(stream_output_dir_path, "*.m3u8")):
        os.remove(old_file)
    print(f"Cleaned up old HLS files in {stream_output_dir_path}")

    ffmpeg_process = None

    try:
        # 영상 프레임별 분석
        cap = cv2.VideoCapture(video_path)

        if not cap.isOpened():
            error_msg = f"Failed to open video file: {video_path}. The file may be corrupted or in an unsupported format."
            print(f"ERROR: {error_msg}")
            return JSONResponse(status_code=500, content={"error": error_msg})

        fps = int(cap.get(cv2.CAP_PROP_FPS))
        frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        print(f"Video properties: FPS={fps}, Width={frame_width}, Height={frame_height}")

        if fps == 0 or frame_width == 0 or frame_height == 0:
            error_msg = f"Invalid video properties: FPS={fps}, Width={frame_width}, Height={frame_height}"
            print(f"ERROR: {error_msg}")
            cap.release()
            return JSONResponse(status_code=500, content={"error": error_msg})

        # 영상 리사이즈 설정 (메모리 절약을 위해 480p로 축소)
        max_height = 480
        resize_needed = frame_height > max_height
        if resize_needed:
            resize_scale = max_height / frame_height
            new_width = int(frame_width * resize_scale)
            new_height = max_height
            print(f"Resizing video: {frame_width}x{frame_height} -> {new_width}x{new_height} (scale: {resize_scale:.2f})")
        else:
            new_width = frame_width
            new_height = frame_height
            print(f"No resize needed: {frame_width}x{frame_height}")

        # 짝수로 맞추기 (H.264 요구사항)
        new_width = new_width if new_width % 2 == 0 else new_width - 1
        new_height = new_height if new_height % 2 == 0 else new_height - 1

        # FFmpeg 프로세스 시작 (라이브 HLS 스트리밍 모드)
        ffmpeg_cmd = [
            "ffmpeg",
            "-y",  # 덮어쓰기
            "-f", "rawvideo",  # 입력 포맷: raw video
            "-vcodec", "rawvideo",
            "-pix_fmt", "bgr24",  # OpenCV 기본 포맷
            "-s", f"{new_width}x{new_height}",  # 해상도
            "-r", str(fps),  # 프레임레이트
            "-i", "-",  # stdin에서 입력 받음
            "-c:v", "libx264",  # H.264 인코딩
            "-preset", "ultrafast",  # 가장 빠른 인코딩 (실시간용)
            "-tune", "zerolatency",  # 저지연 튜닝
            "-crf", "28",  # 품질
            "-g", str(fps * 2),  # GOP 크기 (2초)
            "-hls_time", "2",  # 2초 세그먼트
            "-hls_list_size", "0",  # 모든 세그먼트 유지 (라이브 모드)
            "-hls_flags", "append_list+omit_endlist",  # ENDLIST 태그 생략 (라이브 모드)
            "-hls_segment_filename", os.path.join(stream_output_dir_path, "segment_%03d.ts"),
            hls_output
        ]

        print(f"Starting FFmpeg process for live HLS streaming...")
        ffmpeg_process = subprocess.Popen(
            ffmpeg_cmd,
            stdin=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=10**8
        )
        print(f"FFmpeg process started (PID: {ffmpeg_process.pid})")

        frame_count = 0
        analysis_interval = fps * 1  # 1초마다 분석

        analyzed_frames = []

        print(f"Analysis interval: Every {analysis_interval} frames (every 1 second)")

        last_detections = []

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            # 프레임 리사이즈
            if resize_needed or frame.shape[1] != new_width or frame.shape[0] != new_height:
                frame = cv2.resize(frame, (new_width, new_height))

            if frame_count % analysis_interval == 0:
                # 임시 프레임 저장
                temp_frame_path = os.path.join(SAVE_DIR, f"temp_frame_{cctv_id}.jpg")
                cv2.imwrite(temp_frame_path, frame, [cv2.IMWRITE_JPEG_QUALITY, 85])

                # YOLO 기본 탐지 실행
                yolo_result = yolo.predict(temp_frame_path, conf_threshold=conf_threshold, save_path=None)

                if "detections" in yolo_result:
                    # Human 객체에 대해 Pose 분류 실행
                    final_detections = []

                    for det in yolo_result.get("detections", []):
                        if det.get("class") == "human":
                            # Human 박스 크롭
                            box = det.get("box", {})
                            x1, y1, x2, y2 = map(int, [box.get("x1"), box.get("y1"), box.get("x2"), box.get("y2")])
                            cropped_img = frame[y1:y2, x1:x2]

                            if cropped_img.size > 0 and pose_model is not None:
                                # Pose 분류
                                detected_pose, pose_score = pose_model.predict_pose(cropped_img, pose_conf_threshold)
                                det["pose"] = detected_pose
                                det["pose_score"] = pose_score

                                # 메모리 해제
                                del cropped_img
                            elif pose_model is None:
                                det["pose"] = "Unknown (Model Not Loaded)"
                                det["pose_score"] = 0.0

                        final_detections.append(det)

                    # Spring Boot에 맞게 detections 필드명 변환
                    formatted_detections = []
                    for det in final_detections:
                        formatted_det = {
                            "className": det.get("class"),
                            "confidence": det.get("confidence"),
                            "box": det.get("box"),
                            "pose": det.get("pose"),
                            "poseScore": det.get("pose_score")
                        }
                        formatted_detections.append(formatted_det)

                    formatted_summary = {
                        "fireCount": yolo_result.get("summary", {}).get("fire_count", 0),
                        "humanCount": yolo_result.get("summary", {}).get("human_count", 0),
                        "smokeCount": yolo_result.get("summary", {}).get("smoke_count", 0),
                        "totalObjects": yolo_result.get("summary", {}).get("total_objects", 0)
                    }

                    # Spring Boot API 호출
                    try:
                        payload = {
                            "aiResult": {
                                "imagePath": temp_frame_path,
                                "detections": formatted_detections,
                                "summary": formatted_summary
                            },
                            "cctvId": cctv_id,
                            "locationId": location_id,
                            "videoUrl": f"{STREAM_BASE_URL}/streams/cctv{cctv_id}/playlist.m3u8"
                        }

                        print(f"\n=== Sending to Spring Boot (Frame {frame_count}) ===")
                        print(f"cctvId: {cctv_id}, locationId: {location_id}, Detections: {len(formatted_detections)}")

                        response = requests.post(
                            f"{spring_boot_url}/detections/ai-analysis",
                            json=payload,
                            timeout=5
                        )

                        if response.status_code == 200:
                            print(f"Frame {frame_count}: Sent to Spring Boot successfully")
                        else:
                            print(f"Frame {frame_count}: Spring Boot API error - {response.status_code}")

                    except Exception as e:
                        print(f"Frame {frame_count}: Failed to send to Spring Boot - {str(e)}")

                    analyzed_frames.append({
                        "frame_number": frame_count,
                        "timestamp": frame_count / fps,
                        "detections": final_detections,
                        "summary": yolo_result.get("summary", {})
                    })

                    # 바운딩 박스가 그려진 프레임 저장
                    last_detections = final_detections

            # 모든 프레임에 바운딩 박스 그리기 (마지막 분석 결과 사용)
            if last_detections:
                annotated_frame = draw_pose_predictions(frame, last_detections)
            else:
                annotated_frame = frame.copy()

            # FFmpeg로 프레임 전송 (실시간 HLS 생성)
            try:
                ffmpeg_process.stdin.write(annotated_frame.tobytes())
            except BrokenPipeError:
                print("FFmpeg pipe broken, stopping...")
                break

            # 메모리 해제
            del annotated_frame
            del frame

            frame_count += 1

            # 진행 상황 출력 (100프레임마다)
            if frame_count % 100 == 0:
                print(f"Processed {frame_count} frames, HLS segments being generated...")

        cap.release()
        print(f"Video analysis completed: {frame_count} frames processed")

        # FFmpeg 프로세스 종료
        if ffmpeg_process:
            ffmpeg_process.stdin.close()
            ffmpeg_process.wait(timeout=30)
            print(f"FFmpeg process finished")

        # 라이브 HLS 모드: #EXT-X-ENDLIST를 추가하지 않음
        # 프론트엔드 HLS.js가 라이브 모드로 계속 동작하며,
        # 새 세그먼트가 없으면 자연스럽게 버퍼링/대기 상태가 됨
        print(f"Live HLS mode: #EXT-X-ENDLIST not added (stream stays live)")

    except Exception as e:
        error_msg = f"Video analysis failed: {str(e)}"
        print(f"ERROR: {error_msg}")
        print(traceback.format_exc())
        if ffmpeg_process:
            ffmpeg_process.kill()
        return JSONResponse(status_code=500, content={"error": error_msg, "traceback": traceback.format_exc()})

    print(f"=== Video Analysis Summary ===")
    print(f"Total frames: {frame_count}")
    print(f"Analyzed frames: {len(analyzed_frames)}")
    print(f"HLS playlist: /streams/cctv{cctv_id}/playlist.m3u8")
    print("=" * 50)

# ----------------- RUN -----------------
if __name__ == "__main__":
    uvicorn.run("service:app", host="0.0.0.0", port=8000, reload=True)
