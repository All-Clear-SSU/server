package opensource.project.config;

import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j // 로깅을 위한 Log 객체 생성을 위해
@RestControllerAdvice   // RestController에서 발생하는 예외 가로챔
public class GlobalExceptionHandler {

    // @Valid로 된 DTO 검증 실패 시 예외 처리(첫 번째 에러 메시지 추출해 클라이언트에게 반환)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("=== Validation Error ===");
        e.getBindingResult().getFieldErrors().forEach(error -> {
            log.error("Field: {}, Rejected Value: {}, Message: {}",
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage());
        });
        log.error("=========================");

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        ErrorResponse error = new ErrorResponse(message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Service 계층에서 비즈니스 규칙 위반 시 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 이외의 모든 예외들 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        // 배포 중에는 실제 에러 메시지를 반환하지 않도록 "Internal Server error"로 표시로 변경하기(보안 위험)
        // 현재는 개발 편의를 위해 실제 에러 메시지 반환
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
//        String errorMessage = "Internal server error";
        ErrorResponse error = new ErrorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
