package com.example.jobpuzzle.global.common;

import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true,"success","요청이 성공했습니다.",data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode){
        return new ApiResponse<>(
                false, errorCode.getCode(), errorCode.getMessage(),null
                );
    }
}
