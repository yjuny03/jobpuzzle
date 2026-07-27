package com.example.jobpuzzle.global.error;

import com.example.jobpuzzle.global.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e){
        ErrorCode errorCode = e.getErrorCode();
        // 내부 진단 메시지는 로그에만 남기고 AI 계약/Provider 상세를 공개 응답으로 전달하지 않는다.
        log.error("handled custom exception errorCode={} technicalMessage={}",
                errorCode.getCode(), e.getMessage(), e);
        String publicMessage = errorCode == ErrorCode.AI_RESPONSE_INVALID
                ? errorCode.getMessage()
                : e.getMessage();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode, publicMessage));
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

    // 업로드 파일이 10MB를 초과함
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e){
        return ResponseEntity
                .status(ErrorCode.FILE_TOO_LARGE.getStatus())
                .body(ApiResponse.fail(ErrorCode.FILE_TOO_LARGE));
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
