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
    // 자세한 정보 원하면 debug -> info로 바꾸기
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("=== Incoming Request ===");
        log.debug("Method: {}", request.getMethod());
        log.debug("URI: {}", request.getRequestURI());
        log.debug("Content-Type: {}", request.getContentType());

        // POST 요청의 경우 body 로깅
        if ("POST".equalsIgnoreCase(request.getMethod()) && request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.debug("Request Body: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);
            }
        }

        log.debug("=======================");
        return true;
    }

    // Controller 실행 완료 후 호출 - 응답 상태코드 및 예외 로깅
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.debug("=== Response ===");
        log.debug("Status: {}", response.getStatus());
        if (ex != null) {
            log.error("Exception occurred: ", ex);
        }
        log.debug("================");
    }
}