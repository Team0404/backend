package com.sparta.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 응답 포맷.
 *
 * <pre>
 * { "success": true, "code": "OK", "message": "...", "data": { ... } }
 * </pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "요청이 정상 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "OK", "요청이 정상 처리되었습니다.", null);
    }

    public static ApiResponse<Void> error(ErrorCodeIfs errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> error(ErrorCodeIfs errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}
