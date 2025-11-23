package opensource.project.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

// HTTP 요청/응답 로깅을 위한 인터셉터
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    // Controller 실행 전 호출 - 요청 정보(Method, URI, Body) 로깅
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("=== Incoming Request ===");
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("Content-Type: {}", request.getContentType());

        // POST 요청의 경우 body 로깅
        if ("POST".equalsIgnoreCase(request.getMethod()) && request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.info("Request Body: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);
            }
        }

        log.info("=======================");
        return true;
    }

    // Controller 실행 완료 후 호출 - 응답 상태코드 및 예외 로깅
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("=== Response ===");
        log.info("Status: {}", response.getStatus());
        if (ex != null) {
            log.error("Exception occurred: ", ex);
        }
        log.info("================");
    }
}