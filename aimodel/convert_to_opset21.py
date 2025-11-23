#!/usr/bin/env python3
"""
YOLO 모델을 ONNX opset 21로 변환하는 스크립트
"""
import os
from pathlib import Path

try:
    from ultralytics import YOLO
except ImportError:
    print("ERROR: ultralytics 패키지가 설치되지 않았습니다.")
    print("다음 명령으로 설치하세요: pip install ultralytics")
    exit(1)

# 현재 스크립트의 디렉토리
BASE_DIR = Path(__file__).parent
MODEL_DIR = BASE_DIR / "model"

# PyTorch 모델 파일 (가장 좋은 성능의 모델 사용)
PT_MODEL = MODEL_DIR / "90-fire_80-human_70-smoke_yolo11n.pt"

# 출력 ONNX 파일
OUTPUT_ONNX = MODEL_DIR / "best_noise.onnx"

print(f"PyTorch 모델 로드 중: {PT_MODEL}")
if not PT_MODEL.exists():
    print(f"ERROR: PyTorch 모델 파일을 찾을 수 없습니다: {PT_MODEL}")
    exit(1)

# YOLO 모델 로드
model = YOLO(str(PT_MODEL))

print(f"ONNX로 변환 중 (opset=21)...")
# ONNX로 export
success = model.export(
    format='onnx',
    opset=21,  # opset 21로 지정 (ONNX Runtime 1.20.1이 지원)
    simplify=True,
    dynamic=False,
    imgsz=640
)

if success:
    # ultralytics는 자동으로 .onnx 파일을 생성하지만 이름이 다를 수 있음
    # 생성된 파일을 best_noise.onnx로 복사
    generated_onnx = PT_MODEL.with_suffix('.onnx')
    if generated_onnx.exists():
        import shutil
        shutil.copy(generated_onnx, OUTPUT_ONNX)
        print(f"✅ 변환 완료: {OUTPUT_ONNX}")
        print(f"파일 크기: {OUTPUT_ONNX.stat().st_size / (1024*1024):.2f} MB")

        # 기존 생성 파일 삭제 (중복 방지)
        if generated_onnx != OUTPUT_ONNX:
            generated_onnx.unlink()
    else:
        print(f"경고: 변환된 파일을 찾을 수 없습니다: {generated_onnx}")
else:
    print("❌ 변환 실패")
    exit(1)

# Pose 모델도 변환 (존재하는 경우)
POSE_PT_MODEL = MODEL_DIR / "pose_model.pt"
POSE_ONNX = MODEL_DIR / "pose_model.onnx"

if POSE_PT_MODEL.exists():
    print(f"\nPose 모델 변환 중: {POSE_PT_MODEL}")
    pose_model = YOLO(str(POSE_PT_MODEL))
    pose_success = pose_model.export(
        format='onnx',
        opset=21,
        simplify=True,
        dynamic=False,
        imgsz=640
    )

    if pose_success:
        generated_pose = POSE_PT_MODEL.with_suffix('.onnx')
        if generated_pose.exists() and generated_pose != POSE_ONNX:
            import shutil
            shutil.copy(generated_pose, POSE_ONNX)
            generated_pose.unlink()
        print(f"✅ Pose 모델 변환 완료: {POSE_ONNX}")
else:
    print(f"\nPose 모델을 찾을 수 없습니다 (스킵): {POSE_PT_MODEL}")

print("\n모든 변환 완료!")