"""
===============================================================================
[수정 이력] yolo_pose_model.py - 원본 대비 변경사항
===============================================================================
1. 클래스 docstring 업데이트 (line 8)
   - 원본: 'Standing', 'Sitting', 'Fall' 중 하나로 분류 (3개 클래스)
   - 수정: 'Crawling', 'Falling', 'Sitting', 'Standing' 중 하나로 분류 (4개 클래스)
   - 이유: 새 모델이 4개 클래스를 지원하며, Fall → Falling으로 명칭 변경

2. ONNX 세션 provider 동적 설정 (line 21-28)
   - 원본: providers=['CUDAExecutionProvider', 'CPUExecutionProvider'] (고정)
   - 수정: ort.get_available_providers()로 사용 가능한 provider만 동적 선택
   - 이유: EC2(GPU 없음)에서 "CUDAExecutionProvider is not available" 경고 제거

3. predict_pose() 반환값 설명 업데이트 (line 81)
   - 원본: :return: (str: "Fall", float: 0.95)
   - 수정: :return: (str: "Falling", float: 0.95) - Crawling, Falling, Sitting, Standing 중 하나
   - 이유: 클래스명 변경 반영

4. 출력 처리 주석 업데이트 (line 93)
   - 원본: 출력: [[score1, score2, score3]] (3개)
   - 수정: 출력: [[score1, score2, score3, score4]] (Crawling, Falling, Sitting, Standing)
   - 이유: 4개 클래스 출력 반영

5. valid_scores 슬라이싱 추가 (line 97-100)
   - 원본: top_class_id = np.argmax(scores), top_score = float(scores[top_class_id])
   - 수정: valid_scores = scores[:len(self.class_names)]로 정의된 클래스 수만큼만 사용
   - 이유: 모델 출력이 클래스 수보다 많을 경우 대비 (안정성 향상)

6. 디버깅 로그 추가 (line 102-107)
   - 원본: print(f"Pose model scores: {scores}") (단순 출력)
   - 수정: 모든 클래스별 점수를 개별 출력하는 상세 디버깅 로그
   - 이유: 모델 동작 검증 및 문제 해결 용이

7. Sitting/Standing 보정 로직 추가 (line 112-132, 현재 비활성화)
   - 원본: 없음
   - 수정: Sitting이 선택되었지만 Standing 점수가 15% 이상이면 재고려하는 로직
   - 이유: 모델이 Standing을 Sitting으로 오분류하는 경우 완화 (현재 테스트를 위해 비활성화)
===============================================================================
"""
import cv2
import numpy as np
import onnxruntime as ort

class YOLOPoseOnnx:
    """
    ONNX 기반의 자세 '분류(Classification)' 모델을 처리하기 위한 전용 클래스.
    크롭된 'human' 이미지를 입력받아 'Crawling', 'Falling', 'Sitting', 'Standing' 중 하나로 분류합니다.
    """

    def __init__(self, model_path, class_names, input_shape=(640, 640)):
        """
        모델과 세션을 초기화
        :param model_path: .onnx 모델 파일 경로
        :param class_names: ["Crawling", "Falling", "Sitting", "Standing"]
        :param input_shape: 모델이 요구하는 입력 이미지 크기 (정사각형 가정)
        """
        self.class_names = class_names
        self.input_shape = input_shape

        # [수정] 원본: providers=['CUDAExecutionProvider', 'CPUExecutionProvider'] (고정)
        # ONNX 런타임 세션 생성 (사용 가능한 provider만 사용) -> EC2 내에는 GPU가 없어 CUDA 사용 불가, 대신 CPU로 동작(느림)
        available_providers = ort.get_available_providers()
        providers = []
        if 'CUDAExecutionProvider' in available_providers:
            providers.append('CUDAExecutionProvider')
        providers.append('CPUExecutionProvider')

        self.session = ort.InferenceSession(model_path, providers=providers)

        # 모델의 입력/출력 이름 가져오기
        self.input_name = self.session.get_inputs()[0].name
        self.output_name = self.session.get_outputs()[0].name

        print(f"Pose Model loaded: {model_path}")
        print(f"Pose Input: {self.input_name}, Output: {self.output_name}")
        print(f"Pose Classes: {self.class_names}")


    def preprocess_image(self, image_bgr):
        """
        입력 이미지를 모델에 맞게 전처리합니다. (YOLOv5/v8 기준)
        """
        # 1. 레터박스로 리사이즈
        h, w = image_bgr.shape[:2]
        target_h, target_w = self.input_shape

        r = min(target_h / h, target_w / w)
        new_h, new_w = int(h * r), int(w * r)

        resized_img = cv2.resize(image_bgr, (new_w, new_h), interpolation=cv2.INTER_LINEAR)

        # 2. 패딩 추가
        top = (target_h - new_h) // 2
        bottom = target_h - new_h - top
        left = (target_w - new_w) // 2
        right = target_w - new_w - left

        padded_img = cv2.copyMakeBorder(resized_img, top, bottom, left, right,
                                        cv2.BORDER_CONSTANT, value=(114, 114, 114))

        # 3. BGR -> RGB
        image_rgb = cv2.cvtColor(padded_img, cv2.COLOR_BGR2RGB)

        # 4. HWC -> CHW
        image_chw = np.transpose(image_rgb, (2, 0, 1))

        # 5. 정규화 (0-255 -> 0.0-1.0)
        image_normalized = image_chw.astype(np.float32) / 255.0

        # 6. 배치 차원 추가 (CHW -> NCHW)
        input_tensor = np.expand_dims(image_normalized, axis=0)

        return input_tensor

    def predict_pose(self, cv2_image, conf_threshold=0.3):
        """
        크롭된 CV2 이미지를 입력받아, 가장 확률이 높은 자세와 점수를 반환합니다.

        :param cv2_image: 크롭된 'human' 이미지 (numpy array)
        :param conf_threshold: 최소 신뢰도
        :return: (str: "Falling", float: 0.95) - Crawling, Falling, Sitting, Standing 중 하나
        """
        if cv2_image is None or cv2_image.size == 0:
            return "Unknown (Image Error)", 0.0

        # 1. 이미지 전처리
        input_tensor = self.preprocess_image(cv2_image)

        # 2. ONNX 추론 실행
        outputs = self.session.run([self.output_name], {self.input_name: input_tensor})

        # 3. 출력 처리 (분류 모델)
        # 출력: [[score1, score2, score3, score4]] (Crawling, Falling, Sitting, Standing)
        scores = outputs[0][0]

        # 4. 가장 높은 점수의 인덱스 찾기 (ArgMax)
        # 정의된 클래스 수(4개: Crawling, Falling, Sitting, Standing)만큼만 사용
        valid_scores = scores[:len(self.class_names)]
        top_class_id = np.argmax(valid_scores)
        top_score = float(valid_scores[top_class_id])

        # 디버깅: 모든 클래스별 점수 출력 (로그 확인용)
        print(f"\n[Pose Debug] All scores:")
        for i, class_name in enumerate(self.class_names):
            if i < len(valid_scores):
                print(f"  {class_name}: {valid_scores[i]:.4f}")
        print(f"[Pose Debug] Selected: {self.class_names[top_class_id]} (score: {top_score:.4f})")

        if top_score < conf_threshold:
            return "Unknown (Low Conf)", top_score

        # ============ Sitting/Standing 보정 로직 (임시 비활성화) ============
        # Sitting 점수가 높지만, Standing도 어느 정도 있다면 재고려
        # (모델이 Standing을 잘 못 잡는 문제 완화)
        # 클래스 순서: ["Crawling", "Falling", "Sitting", "Standing"]
        # if len(valid_scores) >= 4:
        #     sitting_idx = self.class_names.index("Sitting") if "Sitting" in self.class_names else -1
        #     standing_idx = self.class_names.index("Standing") if "Standing" in self.class_names else -1
        #
        #     if sitting_idx >= 0 and standing_idx >= 0:
        #         sitting_score = valid_scores[sitting_idx]
        #         standing_score = valid_scores[standing_idx]
        #
        #         # Sitting이 선택되었지만 Standing 점수가 15% 이상이면 재고려
        #         # (서 있는 사람이 Sitting으로 오분류되는 경우 완화)
        #         if top_class_id == sitting_idx and standing_score > 0.15:
        #             score_diff = sitting_score - standing_score
        #             # 점수 차이가 크지 않으면 (0.6 이하) Unknown 처리
        #             if score_diff < 0.6:
        #                 print(f"[WARN] Ambiguous prediction: Sitting={sitting_score:.4f}, Standing={standing_score:.4f}")
        #                 return "Unknown (Ambiguous)", top_score
        # ======================================================================

        top_class_name = self.class_names[top_class_id]

        return top_class_name, top_score