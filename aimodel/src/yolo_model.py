import cv2
import numpy as np
import psutil, os, time
from ultralytics import YOLO

class YOLOOnnx:
    def __init__(self, custom_model_path, coco_model_path, class_names, input_size=320):
        """
        :param custom_model_path: 불, 연기, (재난 특화)사람 학습 모델 경로
        :param coco_model_path: 일반 사람(COCO Pretrained) 학습 모델 경로
        :param class_names: Custom 모델의 클래스 이름 리스트 (예: ["fire", "human", "smoke"])
        """
        print(f"🔄 Loading Custom Model: {custom_model_path}")
        self.custom_model = YOLO(custom_model_path, task='detect')
        print(f"🔄 Loading COCO Model (Human Expert): {coco_model_path}")
        self.coco_model = YOLO(coco_model_path, task='detect')

        self.class_names = class_names # ["fire", "human", "smoke"]
        self.process = psutil.Process(os.getpid())
        self.input_size = input_size

    def predict(self, img_path, conf_threshold=0.5, nms_threshold=0.4, save_path="predicted_img.jpg"):
        img = cv2.imread(img_path)
        if img is None:
            raise ValueError(f"Image not found at {img_path}")

        # ---------------------------------------------------------
        # 1. Inference (두 모델 병렬 실행 효과)
        # ---------------------------------------------------------

        # A. Custom Model (Fire, Smoke, Human)
        # time_start = time.time()
        custom_results = self.custom_model(img, conf=conf_threshold, iou=nms_threshold, verbose=False)[0]
        # end_custom = time.time()
        # print(f"Custom Model Inference Time: {(end_custom - time_start)*1000:.2f} ms")

        # B. COCO Model (Only Class 0 = Person)
        # classes=[0] 옵션으로 '사람'만 탐지하도록 필터링
        # time_start_coco = time.time()
        coco_results = self.coco_model(img, conf=conf_threshold, iou=nms_threshold, classes=[0], verbose=False)[0]

        # end_coco = time.time()
        # print(f"COCO Model Inference Time: {(end_coco - time_start_coco)*1000:.2f} ms")
        # ---------------------------------------------------------
        # 2. Result Fusion (결과 병합)
        # ---------------------------------------------------------
        final_boxes = []

        # [Step 1] Fire & Smoke는 Custom Model 결과를 무조건 신뢰 (그대로 추가)
        # Human은 별도로 모아서 NMS 처리를 해야 함 (중복 제거)
        human_candidates_boxes = []   # [x1, y1, x2, y2]
        human_candidates_scores = []  # confidence

        # Custom 결과 파싱
        for box in custom_results.boxes:
            cls_id = int(box.cls)
            conf = float(box.conf)
            xyxy = box.xyxy[0].cpu().numpy().astype(int)

            # class_names 예: ["fire", "human", "smoke"] 라고 가정
            # 인덱스 안전장치
            if cls_id >= len(self.class_names): continue
            cls_name = self.class_names[cls_id]

            if cls_name == "human":
                # 사람인 경우 후보군에 등록
                human_candidates_boxes.append(xyxy.tolist())
                human_candidates_scores.append(conf)
            else:
                # fire, smoke는 즉시 최종 결과에 포함
                final_boxes.append({
                    "class": cls_name,
                    "confidence": conf,
                    "box": {"x1": xyxy[0], "y1": xyxy[1], "x2": xyxy[2], "y2": xyxy[3]}
                })

        # COCO 결과 파싱 (여긴 무조건 사람임)
        for box in coco_results.boxes:
            conf = float(box.conf)
            xyxy = box.xyxy[0].cpu().numpy().astype(int)

            # COCO 모델의 사람은 후보군에 등록
            human_candidates_boxes.append(xyxy.tolist())
            human_candidates_scores.append(conf)

        # ---------------------------------------------------------
        # 3. Apply NMS for Humans (사람 중복 제거)
        # ---------------------------------------------------------
        if human_candidates_boxes:
            # cv2.dnn.NMSBoxes는 [x, y, w, h] 포맷을 원하므로 변환 필요
            boxes_xywh = []
            for (x1, y1, x2, y2) in human_candidates_boxes:
                boxes_xywh.append([x1, y1, x2 - x1, y2 - y1])

            # NMS 실행
            indices = cv2.dnn.NMSBoxes(boxes_xywh, human_candidates_scores, conf_threshold, nms_threshold)

            # 살아남은 사람 박스만 최종 결과에 추가
            if len(indices) > 0:
                for i in indices.flatten():
                    x1, y1, x2, y2 = human_candidates_boxes[i]
                    conf = human_candidates_scores[i]
                    final_boxes.append({
                        "class": "human", # 통일된 클래스명
                        "confidence": conf,
                        "box": {"x1": x1, "y1": y1, "x2": x2, "y2": y2}
                    })

        # ---------------------------------------------------------
        # 4. Visualization & Output Formatting
        # ---------------------------------------------------------
        vis = img.copy()
        summary = {name: 0 for name in self.class_names} # 초기화

        for item in final_boxes:
            cls_name = item["class"]
            conf = item["confidence"]
            box = item["box"]
            x1, y1, x2, y2 = box["x1"], box["y1"], box["x2"], box["y2"]

            # 통계 업데이트
            if cls_name in summary:
                summary[cls_name] += 1
            else:
                # Custom 모델 클래스에 없는게 들어올 경우(거의 없겠지만)
                summary.setdefault(cls_name, 0)
                summary[cls_name] += 1

            # Draw bounding box
            # 색상: Human(Green), Fire(Red), Smoke(Gray/Orange)
            if cls_name == "human":
                color = (0, 255, 0)
            elif "fire" in cls_name.lower():
                color = (0, 0, 255)
            elif "smoke" in cls_name.lower():
                color = (128, 128, 128)
            else:
                color = (255, 255, 0)

            cv2.rectangle(vis, (x1, y1), (x2, y2), color, 2)

            # Label
            label = f"{cls_name}: {conf:.2f}"
            (w, h), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)
            cv2.rectangle(vis, (x1, y1 - 20), (x1 + w, y1), color, -1)
            cv2.putText(vis, label, (x1, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        if save_path:
            cv2.imwrite(save_path, vis)

        return {
            "image_path": save_path,
            "detections": final_boxes,
            "summary": {
                "fire_count": summary.get("fire", 0),
                "human_count": summary.get("human", 0),
                "smoke_count": summary.get("smoke", 0),
                "total_objects": len(final_boxes)
            }
        }

    def benchmark(self, img_path, runs=10):
        """
        하이브리드(Custom + COCO + NMS) 파이프라인의 실제 속도를 측정합니다.
        """
        import time
        img = cv2.imread(img_path)
        if img is None:
            print("❌ Image not found for benchmark.")
            return

        print(f"\n🚀 Running Hybrid Benchmark ({runs} runs)...")
        print(f"• Custom Model: {self.custom_model.model.pt_path if hasattr(self.custom_model.model, 'pt_path') else 'Custom'}")
        print(f"• COCO Model: {self.coco_model.model.pt_path if hasattr(self.coco_model.model, 'pt_path') else 'COCO'}")

        # Warm-up (초기 로딩 시간 제외)
        print("• Warming up...")
        self.predict(img_path, conf_threshold=0.5)

        times = []
        for i in range(runs):
            start_t = time.time()

            # 실제 예측 파이프라인 실행
            _ = self.predict(img_path, conf_threshold=0.5)

            end_t = time.time()
            times.append((end_t - start_t) * 1000) # ms 단위
            print(f"  - Run {i+1}: {times[-1]:.2f} ms")

        avg_time = sum(times) / runs
        fps = 1000 / avg_time

        print(f"\n📊 Benchmark Result")
        print(f"• Avg Processing Time : {avg_time:.2f} ms")
        print(f"• Estimated FPS       : {fps:.2f} FPS")

        if fps < 5:
            print("⚠️ WARNING: FPS가 너무 낮습니다. 실시간 데모가 위험할 수 있습니다.")
            print("💡 TIP: COCO 모델을 'n'이나 's' 버전으로 교체하거나 input_size를 줄이세요.")
        else:
            print("✅ Status: 데모 시연에 적합한 속도입니다.")


# if __name__ == "__main__":
#     # 테스트용 메인
#     MODEL_PATH = "./model/best_human.onnx"
#     COCO_MODEL_PATH = "./model/yolo11m.onnx"
#     TEST_FILE = "./human_cctv.png"
#     class_names = ["fire", "human", "smoke"]                    # Custom 모델 클래스 이름
#
#     yolo_onnx = YOLOOnnx(MODEL_PATH, COCO_MODEL_PATH, class_names)
#     result = yolo_onnx.predict(TEST_FILE, conf_threshold=0.5,
#                                 save_path="./predicted_human.png")
#     print("Detections:", result["detections"])
#     print("Summary:", result["summary"])
#     yolo_onnx.benchmark(TEST_FILE, runs=100)