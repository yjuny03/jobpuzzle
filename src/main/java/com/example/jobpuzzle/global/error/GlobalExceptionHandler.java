package com.example.jobpuzzle.global.error;

import com.example.jobpuzzle.global.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e){
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    // @Valid 검증 실패 (JoinRequest/LoginRequest/MyInfoUpdateRequest 등의 @NotBlank, @Email 등) - 첫 번째 오류 메시지를 그대로 보여줌
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ErrorCode.INVALID_REQUEST.getCode(), message, null));
    }

    // 존재하지 않는 경로/리소스 요청 - 이전엔 아래 Exception 핸들러에 걸려서 500(서버 내부 오류)으로 잘못 나갔던 걸 분리
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e){
        return ResponseEntity
                .status(ErrorCode.COMMON_NOT_FOUND.getStatus())
                .body(ApiResponse.fail(ErrorCode.COMMON_NOT_FOUND));
    }

    // 예상 못한 예외 - 화면엔 뭉뚱그려서 보여주되, 콘솔엔 실제 원인이 남게 로그를 찍음 (이게 없어서 지금까지 원인 파악이 안 됐음)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e){
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
