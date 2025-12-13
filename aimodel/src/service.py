# NOTE:
# - Refactored based on your provided full service.py
# - DEMO MODE is enabled by default (FIXED 4 CCTV streams)
# - FUTURE: Dynamic stream mode can be enabled by COMMENT / UNCOMMENT only
#   (search for "FUTURE: DYNAMIC STREAM MODE")

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import cv2
import numpy as np
import os
import threading
from typing import Dict, Optional
from datetime import datetime
import requests

from yolo_model import YOLOOnnx
from yolo_pose_model import YOLOPoseOnnx

# ==================================================
# CONFIG
# ==================================================
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

MODEL_PATH = os.path.join(BASE_DIR, "model", "best_human.onnx")
COCO_MODEL_PATH = os.path.join(BASE_DIR, "model", "yolo11m.onnx")
POSE_MODEL_PATH = os.path.join(BASE_DIR, "model", "best_pose.onnx")

CLASS_NAMES = ["fire", "human", "smoke"]
POSE_CLASS_NAMES = ["Crawling", "Falling", "Sitting", "Standing"]

SAVE_DIR = os.path.join(BASE_DIR, "predictions")
os.makedirs(SAVE_DIR, exist_ok=True)

SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")
SPRING_BOOT_AI_ENDPOINT = f"{SPRING_BOOT_URL.rstrip('/')}/detections/ai-analysis"
HLS_BASE_URL = os.getenv("HLS_BASE_URL", "http://localhost:8080/streams")

# ===============================
# DEMO MODE CONFIG (DEFAULT)
# ===============================
MAX_CCTV = 4
STREAM_OUTPUT_DIR = "/home/ubuntu/streams"

# [DEMO MODE – ENABLED]
# 시연에서는 CCTV가 최대 4대이므로 디렉토리를 미리 생성한다.
for i in range(1, MAX_CCTV + 1):
    os.makedirs(os.path.join(STREAM_OUTPUT_DIR, f"cctv{i}"), exist_ok=True)

# [FUTURE: DYNAMIC STREAM MODE]
# 아래 사전 디렉토리 생성 로직을 제거하고
# analyze_live_stream_sync 내부에서 동적 생성하도록 변경한다.

DEFAULT_DETECTION_CONF_THRESHOLD = 0.3
DEFAULT_POSE_CONF_THRESHOLD = 0.3

# ==================================================
# MODEL INIT
# ==================================================
print("[INIT] Loading detection models")
yolo = YOLOOnnx(MODEL_PATH, COCO_MODEL_PATH, CLASS_NAMES)

pose_model = None
try:
    pose_model = YOLOPoseOnnx(POSE_MODEL_PATH, POSE_CLASS_NAMES, input_shape=(640, 640))
    print("[INIT] Pose model loaded")
except Exception as e:
    print(f"[WARN] Pose model disabled: {e}")

# ==================================================
# FASTAPI
# ==================================================
app = FastAPI(title="YOLO ONNX Detection API")

# ==================================================
# REQUEST MODELS
# ==================================================
class StartLiveStreamRequest(BaseModel):
    rtsp_url: str
    cctv_id: int
    location_id: int
    conf_threshold: Optional[float] = DEFAULT_DETECTION_CONF_THRESHOLD
    pose_conf_threshold: Optional[float] = DEFAULT_POSE_CONF_THRESHOLD

class StopLiveStreamRequest(BaseModel):
    cctv_id: int

class StreamStatusResponse(BaseModel):
    cctv_id: int
    is_streaming: bool
    rtsp_url: Optional[str]
    started_at: Optional[str]
    frame_count: Optional[int]

# ==================================================
# STREAM MANAGER
# ==================================================
class StreamManager:
    def __init__(self):
        self.lock = threading.Lock()

        # ===============================
        # DEMO MODE – ENABLED
        # ===============================
        # CCTV 스트림을 1~4번 슬롯으로 고정한다.
        self.streams: Dict[int, Dict] = {
            i: self._create_stream_slot()
            for i in range(1, MAX_CCTV + 1)
        }

        # ===============================
        # FUTURE: DYNAMIC STREAM MODE
        # ===============================
        # 아래 한 줄을 활성화하고 위 DEMO MODE 블록을 주석 처리하면
        # 가변 스트림 구조로 즉시 전환 가능하다.
        # ---------------------------------
        # self.streams: Dict[int, Dict] = {}
        # ---------------------------------

    def _create_stream_slot(self):
        return {
            "is_running": False,
            "thread": None,
            "stop_event": threading.Event(),
            "frame_count": 0,
            "started_at": None,
            "rtsp_url": None,
            "location_id": None,
            "conf_threshold": DEFAULT_DETECTION_CONF_THRESHOLD,
            "pose_conf_threshold": DEFAULT_POSE_CONF_THRESHOLD,
        }

    def _validate_id(self, cctv_id: int):
        # ===============================
        # DEMO MODE – ENABLED
        # ===============================
        if cctv_id not in self.streams:
            raise ValueError("cctv_id must be between 1 and 4 for demo")

        # ===============================
        # FUTURE: DYNAMIC STREAM MODE
        # ===============================
        # 아래 검증을 사용하려면 위 DEMO MODE 검증을 주석 처리한다.
        # ---------------------------------
        # if not isinstance(cctv_id, int):
        #     raise ValueError("cctv_id must be int")
        # ---------------------------------

    def start_stream(self, cctv_id: int, rtsp_url: str, location_id: int,
                     conf_threshold: float, pose_conf_threshold: float) -> bool:
        with self.lock:
            # [FUTURE: DYNAMIC STREAM MODE]
            # if cctv_id not in self.streams:
            #     self.streams[cctv_id] = self._create_stream_slot()

            self._validate_id(cctv_id)
            slot = self.streams[cctv_id]

            if slot["is_running"]:
                return False

            slot.update({
                "is_running": True,
                "rtsp_url": rtsp_url,
                "location_id": location_id,
                "conf_threshold": conf_threshold,
                "pose_conf_threshold": pose_conf_threshold,
                "frame_count": 0,
                "started_at": datetime.now().isoformat(),
            })
            slot["stop_event"].clear()

            t = threading.Thread(
                target=analyze_live_stream_sync,
                args=(
                    rtsp_url,
                    cctv_id,
                    STREAM_OUTPUT_DIR,
                    conf_threshold,
                    pose_conf_threshold,
                    slot["stop_event"],
                    self,
                ),
                daemon=True,
            )
            slot["thread"] = t
            t.start()
            return True

    def stop_stream(self, cctv_id: int) -> bool:
        self._validate_id(cctv_id)
        with self.lock:
            slot = self.streams[cctv_id]
            if not slot["is_running"]:
                return False
            slot["stop_event"].set()
            slot["is_running"] = False
            return True

    def get_status(self, cctv_id: int) -> Dict:
        self._validate_id(cctv_id)
        s = self.streams[cctv_id]
        return {
            "cctv_id": cctv_id,
            "is_streaming": s["is_running"],
            "rtsp_url": s["rtsp_url"],
            "started_at": s["started_at"],
            "frame_count": s["frame_count"],
        }

    def update_frame_count(self, cctv_id: int, count: int):
        self.streams[cctv_id]["frame_count"] = count

stream_manager = StreamManager()

# ==================================================
# API
# ==================================================
@app.get("/health")
def health():
    return {"status": "ok", "mode": "demo-fixed-4"}

@app.post("/start_live_stream")
def start_live_stream(req: StartLiveStreamRequest):
    try:
        ok = stream_manager.start_stream(
            req.cctv_id,
            req.rtsp_url,
            req.location_id,
            req.conf_threshold,
            req.pose_conf_threshold,
        )
        if not ok:
            return JSONResponse(status_code=409, content={"error": "Stream already running"})
        return {"status": "started", "cctv_id": req.cctv_id}
    except ValueError as e:
        return JSONResponse(status_code=400, content={"error": str(e)})

@app.post("/stop_live_stream")
def stop_live_stream(req: StopLiveStreamRequest):
    try:
        ok = stream_manager.stop_stream(req.cctv_id)
        if not ok:
            return JSONResponse(status_code=404, content={"error": "Stream not running"})
        return {"status": "stopped", "cctv_id": req.cctv_id}
    except ValueError as e:
        return JSONResponse(status_code=400, content={"error": str(e)})

@app.get("/stream_status/{cctv_id}", response_model=StreamStatusResponse)
def stream_status(cctv_id: int):
    return stream_manager.get_status(cctv_id)

# ==================================================
# LIVE STREAM CORE
# ==================================================
def analyze_live_stream_sync(
    rtsp_url: str,
    cctv_id: int,
    output_stream_dir: str,
    conf_threshold: float,
    pose_conf_threshold: float,
    stop_event: threading.Event,
    stream_manager: StreamManager,
):
    """
    [DEMO MODE]
    - cctv_id: 1~4 고정
    - HLS 디렉토리는 사전에 생성됨

    [FUTURE: DYNAMIC STREAM MODE]
    - 아래 os.makedirs(out_dir) 주석 해제
    """
    import subprocess, glob
    import time

    out_dir = os.path.join(output_stream_dir, f"cctv{cctv_id}")
    slot_location_id = stream_manager.streams.get(cctv_id, {}).get("location_id")

    # [FUTURE: DYNAMIC STREAM MODE]
    # os.makedirs(out_dir, exist_ok=True)

    playlist = os.path.join(out_dir, "playlist.m3u8")

    for f in glob.glob(os.path.join(out_dir, "*")):
        try:
            os.remove(f)
        except:
            pass

    def open_capture():
        cap_local = cv2.VideoCapture(rtsp_url)
        if not cap_local.isOpened():
            return None, None, None, None
        fps_local = int(cap_local.get(cv2.CAP_PROP_FPS)) or 30
        width = int(cap_local.get(cv2.CAP_PROP_FRAME_WIDTH)) or 1280
        height = int(cap_local.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 720
        return cap_local, fps_local, width, height

    def start_ffmpeg(width, height, fps_local):
        try:
            # 데모 기본 fps는 15로 제한; 필요 시 10~12로 낮춰 CPU/GPU 부하를 줄일 수 있음
            target_fps = max(1, min(int(fps_local or 15), 15))
            return subprocess.Popen([
                "ffmpeg", "-y",
                "-f", "rawvideo", "-pix_fmt", "bgr24",
                "-s", f"{width}x{height}", "-r", str(target_fps),
                "-i", "-",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-crf", "23",
                "-maxrate", "2M",
                "-bufsize", "4M",
                "-g", str(target_fps),
                "-keyint_min", str(target_fps),
                "-sc_threshold", "0",
                "-fflags", "nobuffer",
                "-flags", "low_delay",
                "-flush_packets", "1",
                "-hls_time", "1",
                "-hls_list_size", "4",
                "-hls_flags", "delete_segments+append_list+independent_segments+temp_file",
                "-hls_segment_filename", os.path.join(out_dir, "segment_%03d.ts"),
                "-f", "hls",
                playlist
            ], stdin=subprocess.PIPE)
        except FileNotFoundError:
            print(f"[CCTV {cctv_id}] ffmpeg not found; aborting stream")
            return None

    cap, fps, w, h = open_capture()
    if cap is None:
        print(f"[CCTV {cctv_id}] RTSP open failed; will retry until stopped")

    ffmpeg = start_ffmpeg(w or 1280, h or 720, fps or 30) if cap else None
    frame_count = 0
    consecutive_failures = 0
    max_failures_before_reconnect = 60  # ~2 seconds if 30fps
    last_detections = []
    last_send_ts = 0

    while not stop_event.is_set():
        if cap is None or not cap.isOpened():
            time.sleep(1)
            cap, fps, w, h = open_capture()
            if cap:
                ffmpeg = ffmpeg or start_ffmpeg(w, h, fps or 30)
                consecutive_failures = 0
            continue

        ret, frame = cap.read()
        if not ret or frame is None:
            consecutive_failures += 1
            if consecutive_failures >= max_failures_before_reconnect:
                print(f"[CCTV {cctv_id}] read failures reached {consecutive_failures}; reconnecting")
                cap.release()
                cap = None
            time.sleep(0.1)
            continue

        consecutive_failures = 0

        if frame_count % max(fps, 1) == 0:
            tmp = os.path.join(SAVE_DIR, f"live_{cctv_id}.jpg")
            if not cv2.imwrite(tmp, frame):
                print(f"[CCTV {cctv_id}] Failed to write temp image for detection; skipping")
            else:
                # NOTE: Ultralytics YOLO does NOT allow iou=None
                # Default IoU threshold must be explicitly provided
                try:
                    det_result = yolo.predict(tmp, conf_threshold, 0.45)
                    summary = det_result.get("summary", {})
                    print(f"[CCTV {cctv_id}] YOLO detect fire={summary.get('fire_count')} smoke={summary.get('smoke_count')} human={summary.get('human_count')}")

                    detections = det_result.get("detections", []) or []
                    enriched = []

                    # Pose 분류 (선택, 인간 바운딩 박스만)
                    for item in detections:
                        new_item = dict(item)
                        if new_item.get("class") != "human":
                            enriched.append(new_item)
                            continue
                        box = new_item.get("box", {})
                        x1, y1, x2, y2 = box.get("x1"), box.get("y1"), box.get("x2"), box.get("y2")
                        if None in (x1, y1, x2, y2):
                            enriched.append(new_item)
                            continue
                        # 경계 안전 보정
                        x1, y1 = max(int(x1), 0), max(int(y1), 0)
                        x2, y2 = min(int(x2), frame.shape[1]), min(int(y2), frame.shape[0])
                        if x2 <= x1 or y2 <= y1:
                            enriched.append(new_item)
                            continue
                        new_item["box"] = {"x1": x1, "y1": y1, "x2": x2, "y2": y2}
                        if pose_model:
                            crop = frame[y1:y2, x1:x2]
                            try:
                                pose_label, pose_score = pose_model.predict_pose(crop, conf_threshold=pose_conf_threshold)
                                new_item["pose"] = pose_label
                                new_item["pose_score"] = pose_score
                                print(f"[CCTV {cctv_id}] Pose={pose_label} score={pose_score:.3f} bbox=({x1},{y1},{x2},{y2})")
                            except Exception as e:
                                print(f"[CCTV {cctv_id}] Pose inference error: {e}")
                        enriched.append(new_item)

                    last_detections = enriched
                except Exception as e:
                    print(f"[CCTV {cctv_id}] YOLO inference error:", e)
                else:
                    # 사람이 1명도 없으면 브로드캐스트 스킵 (바운딩박스가 없는 경우 포함)
                    has_human = any(
                        d.get("class") == "human"
                        and d.get("box")
                        and all(v is not None for v in d["box"].values())
                        and (d["box"]["x2"] - d["box"]["x1"]) > 1
                        and (d["box"]["y2"] - d["box"]["y1"]) > 1
                        for d in enriched
                    )
                    if not has_human:
                        # 사람 미탐지 시에도 스트림은 계속 흘려보낸다
                        last_detections = []
                    else:
                        # 성공적으로 추론한 시점에 Spring Boot로 비동기 전송 (1초당 1회 수준)
                        now_ts = time.time()
                        if now_ts - last_send_ts >= 1:
                            last_send_ts = now_ts

                            def to_py_num(val):
                                try:
                                    if isinstance(val, (np.integer,)):
                                        return int(val)
                                    if isinstance(val, (np.floating,)):
                                        return float(val)
                                except Exception:
                                    pass
                                return val

                            payload = {
                                "aiResult": {
                                    "imagePath": det_result.get("image_path"),
                                    "detections": [
                                        {
                                            "className": d.get("class"),
                                            "confidence": to_py_num(d.get("confidence")),
                                            "box": {
                                                "x1": to_py_num(d.get("box", {}).get("x1")),
                                                "y1": to_py_num(d.get("box", {}).get("y1")),
                                                "x2": to_py_num(d.get("box", {}).get("x2")),
                                                "y2": to_py_num(d.get("box", {}).get("y2")),
                                            } if d.get("box") else None,
                                            "pose": d.get("pose"),
                                            "poseScore": to_py_num(d.get("pose_score")),
                                        }
                                        for d in enriched
                                    ],
                                    "summary": {
                                        "fireCount": to_py_num(summary.get("fire_count")),
                                        "humanCount": to_py_num(summary.get("human_count")),
                                        "smokeCount": to_py_num(summary.get("smoke_count")),
                                        "totalObjects": to_py_num(summary.get("total_objects")),
                                    },
                                },
                                "cctvId": to_py_num(cctv_id),
                                "locationId": to_py_num(slot_location_id),
                                "videoUrl": f"{HLS_BASE_URL.rstrip('/')}/cctv{cctv_id}/playlist.m3u8",
                            }

                            def _send_payload(data):
                                try:
                                    resp = requests.post(
                                            SPRING_BOOT_AI_ENDPOINT,
                                            json=data,
                                            timeout=3,
                                    )
                                    if resp.status_code >= 300:
                                        print(f"[CCTV {cctv_id}] SpringBoot AI push failed: {resp.status_code} {resp.text}")
                                except Exception as e:
                                    print(f"[CCTV {cctv_id}] SpringBoot AI push error: {e}")

                            threading.Thread(target=_send_payload, args=(payload,), daemon=True).start()

        # 바운딩 박스/포즈 오버레이 (최근 추론 결과를 사용)
        if last_detections:
            for item in last_detections:
                box = item.get("box", {})
                x1, y1, x2, y2 = box.get("x1"), box.get("y1"), box.get("x2"), box.get("y2")
                if None in (x1, y1, x2, y2):
                    continue
                cls = item.get("class", "obj")
                conf = item.get("confidence", 0.0)
                pose_lbl = item.get("pose")
                pose_score = item.get("pose_score")
                color = (0, 255, 0) if cls == "human" else (0, 0, 255) if "fire" in cls else (128, 128, 128)
                cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
                label = f"{cls}:{conf:.2f}"
                if pose_lbl:
                    label = f"{label} | {pose_lbl}:{pose_score:.2f}"
                (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1)
                cv2.rectangle(frame, (x1, y1 - th - 6), (x1 + tw, y1), color, -1)
                cv2.putText(frame, label, (x1, y1 - 4), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1, cv2.LINE_AA)

        if ffmpeg and ffmpeg.poll() is None:
            try:
                ffmpeg.stdin.write(frame.tobytes())
            except Exception as e:
                print(f"[CCTV {cctv_id}] ffmpeg write error: {e}; restarting ffmpeg")
                try:
                    ffmpeg.terminate()
                except Exception:
                    pass
                ffmpeg = start_ffmpeg(w, h, fps or 30)
        else:
            ffmpeg = start_ffmpeg(w, h, fps or 30)

        frame_count += 1
        if frame_count % 100 == 0:
            stream_manager.update_frame_count(cctv_id, frame_count)

    if cap:
        cap.release()
    if ffmpeg:
        try:
            ffmpeg.stdin.close()
            ffmpeg.terminate()
        except Exception:
            pass
    print(f"[CCTV {cctv_id}] stopped")
