package com.example.backendplantshop.exception;


import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.enums.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j //dùng tạo log.ifo, error... tự động
@ControllerAdvice
public class GlobalExceptionHandler {

    public void logError(Exception e, HttpServletRequest request) {
        log.error("Lỗi call api: " + request.getRequestURI() + " với ERROR: " + e.getClass().getSimpleName());
    }


    //nếu bất kì nơi nào trong controller/service ném AppException thì chạy p/thuc này để xử lý
    @ExceptionHandler(value = BadSqlGrammarException.class)
    ResponseEntity<ApiResponse> handleBadSqlGrammarException(BadSqlGrammarException e, HttpServletRequest request){
        logError(e, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .statusCode(ErrorCode.BAD_SQL.getCode())
                        .success(Boolean.FALSE)
                        .message(ErrorCode.BAD_SQL.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = NullPointerException.class)
    ResponseEntity<ApiResponse> handleNullPointerException(NullPointerException e, HttpServletRequest request){
        logError(e, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .statusCode(ErrorCode.NULL_POINTER.getCode())
                        .success(Boolean.FALSE)
                        .message(ErrorCode.NULL_POINTER.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handleUnKnownException(Exception e, HttpServletRequest request){
        logError(e, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .statusCode(ErrorCode.UNKNOWN_EXCEPTION.getCode())
                        .success(Boolean.FALSE)
                        .message("Lỗi:" + e.getClass().getSimpleName())
                        .build()
        );
    }

    //bắt tất cả appexception
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handleAppException(AppException e, HttpServletRequest request){
        logError(e, request);
        log.info("Xử lý AppException: code={}, message={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .statusCode(e.getErrorCode().getCode())
                        .success(Boolean.FALSE)
                        .message(e.getErrorCode().getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request){
        logError(e, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .statusCode(400)
                        .success(Boolean.FALSE)
                        .message(e.getMessage())
                        .build()
        );
    }

    // ========== SECURITY EXCEPTION HANDLERS ==========
    
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request){
        logError(e, request);
        log.warn("Access denied for user on endpoint: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.builder()
                        .statusCode(ErrorCode.ACCESS_DENIED.getCode())
                        .success(Boolean.FALSE)
                        .message(ErrorCode.ACCESS_DENIED.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = AuthenticationException.class)
    ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException e, HttpServletRequest request){
        logError(e, request);
        log.warn("Authentication failed for endpoint: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .statusCode(401)
                        .success(Boolean.FALSE)
                        .message("Xác thực thất bại. Vui lòng đăng nhập lại")
                        .build()
        );
    }

    @ExceptionHandler(value = BadCredentialsException.class)
    ResponseEntity<ApiResponse> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request){
        logError(e, request);
        log.warn("Bad credentials for endpoint: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .statusCode(401)
                        .success(Boolean.FALSE)
                        .message("Tên đăng nhập hoặc mật khẩu không đúng")
                        .build()
        );
    }


    //bắt lỗi độ dài pass
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }
}
