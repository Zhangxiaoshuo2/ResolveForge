package dev.resolveforge.api;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> business(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(Map.of("code", ex.code(), "message", ex.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", "请求格式或参数无效"));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<?> missing(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("code", "NOT_FOUND", "message", "接口不存在"));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception ex) {
        // Do not log message/body: upstream exceptions may contain tokens or customer data.
        log.error("request_failed type={}", ex.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("code", "INTERNAL_ERROR", "message", "服务暂时不可用"));
    }
}
