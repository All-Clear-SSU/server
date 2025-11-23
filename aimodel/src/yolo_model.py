"""
===============================================================================
[수정 이력] yolo_model.py - 원본 대비 변경사항
===============================================================================
1. 클래스 docstring 추가
   - 원본: docstring 없음
   - 수정: Ultralytics YOLO 기반 객체 탐지 모델 설명 추가

2. YOLO 초기화 시 task='detect' 명시 (line 21)
   - 원본: self.model = YOLO(model_path)
   - 수정: self.model = YOLO(model_path, task='detect')
   - 이유: EC2 배포 시 "Unable to automatically guess model task" 경고 제거

3. 클래스명 소문자 통일 (line 50-52)
   - 원본: "Human", "Fire", "Smoke" (대문자 시작)
   - 수정: "human", "fire", "smoke" (소문자)
   - 이유: service.py의 CLASS_NAMES = ["fire", "human", "smoke"]와 일치시킴

4. summary 필드명 변경 (line 69-73)
   - 원본: summary 딕셔너리 그대로 반환 ({**summary, "total_objects": ...})
   - 수정: fire_count, human_count, smoke_count 명시적 필드명 사용
   - 이유: Spring Boot DetectionService.java와의 호환성 유지

5. 모델 로드 로그 메시지 개선 (line 16)
   - 원본: print(f"YOLO Model loaded: {model_path}")
   - 수정: print(f"YOLO Model loaded (ultralytics): {model_path}")
   - 이유: ultralytics 라이브러리 사용 명시
===============================================================================
"""
import cv2
import numpy as np
import psutil, os, time
from ultralytics import YOLO

class YOLOOnnx:
    """
    Ultralytics YOLO 기반 객체 탐지 모델.
    기존 onnxruntime 직접 사용에서 ultralytics 라이브러리로 전환.
    """
    def __init__(self, model_path, class_names, input_size=640):
        # [수정] 원본: self.model = YOLO(model_path)
        # task='detect' 명시하여 EC2 배포 시 경고 메시지 제거
        self.model = YOLO(model_path, task='detect')
        self.class_names = class_names
        self.process = psutil.Process(os.getpid())
        self.input_size = input_size
        print(f"YOLO Model loaded (ultralytics): {model_path}")
        print(f"Classes: {self.class_names}")

    def preprocess(self, img):
        # ultralytics 내부에서 처리되므로 간단히 리턴
        return img

    def predict(self, img_path, conf_threshold=0.5, nms_threshold=0.4, save_path="predicted_img.jpg"):
        img = cv2.imread(img_path)
        orig_h, orig_w = img.shape[:2]

        # Ultralytics 모델 실행
        results = self.model(img, conf=conf_threshold, iou=nms_threshold)[0]

        boxes_list = []
        summary = {name: 0 for name in self.class_names}

        vis = img.copy()

        for box in results.boxes:
            # 좌표
            x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(int)
            conf = float(box.conf)
            cls_id = int(box.cls)
            cls_name = self.class_names[cls_id] if cls_id < len(self.class_names) else "Unknown"
            summary[cls_name] += 1

            boxes_list.append({
                "class": cls_name,
                "confidence": float(conf),
                "box": {"x1": int(x1), "y1": int(y1), "x2": int(x2), "y2": int(y2)}
            })

            # Draw bounding box
            color = (0,255,0) if cls_name == "human" else \
                    (0,0,255) if cls_name == "fire" else \
                    (255,165,0) if cls_name == "smoke" else (200,200,200)

            cv2.rectangle(vis, (x1,y1), (x2,y2), color, 2)
            label = f"{cls_name}: {conf:.2f}"
            (label_width, label_height), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)
            label_y = y1 - 10 if y1 - 10 > 10 else y1 + 10
            cv2.rectangle(vis, (x1, label_y - label_height), (x1 + label_width, label_y + 5), color, cv2.FILLED)
            cv2.putText(vis, label, (x1, label_y), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,0,0), 2)

        # Save image
        if save_path:
            cv2.imwrite(save_path, vis)

        # 기존 service.py/analyze_video와의 호환성을 위해 fire_count, human_count, smoke_count 필드명 유지
        result = {
            "image_path": save_path,
            "detections": boxes_list,
            "summary": {
                "fire_count": summary.get("fire", 0),
                "human_count": summary.get("human", 0),
                "smoke_count": summary.get("smoke", 0),
                "total_objects": len(boxes_list)
            }
        }

        return result

    def benchmark(self, img_path, runs=100):
        img = cv2.imread(img_path)

        times, cpu_usages, mem_usages = [], [], []

        for _ in range(runs):
            start_t = time.time()
            _ = self.model(img, conf=0.5)[0]
            end_t = time.time()

            times.append((end_t - start_t) * 1000)
            cpu_usages.append(self.process.cpu_percent(interval=0.0))
            mem_usages.append(self.process.memory_info().rss / (1024*1024))

        avg_time = sum(times) / runs
        avg_cpu = sum(cpu_usages) / runs
        avg_mem = sum(mem_usages) / runs
        fps = 1000 / avg_time

        print(f"\n⏱️ Benchmark Result ({runs} runs)")
        print(f"• Avg Inference Time : {avg_time:.2f} ms")
        print(f"• Estimated FPS      : {fps:.2f} FPS")
        print(f"• Avg CPU Usage      : {avg_cpu:.2f} %")
        print(f"• Avg RAM Usage      : {avg_mem:.2f} MB\n")