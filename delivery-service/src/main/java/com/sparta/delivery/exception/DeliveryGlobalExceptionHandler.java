package com.sparta.delivery.exception;

import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>클라이언트가 존재하지 않는 정렬 필드(예: {@code sort=star})를 보내면 예전엔
 * {@link PropertyReferenceException}(Spring Data 파생 쿼리) 또는 {@link IllegalArgumentException}
 * (QueryDSL 수동 정렬 매핑, {@link com.sparta.delivery.repository.DeliveryRepositoryImpl})이 던져졌는데,
 * 둘 다 common의 범용 {@code Exception} 핸들러로 떨어져 500으로 나갔다. 명백한 클라이언트 입력 실수이므로
 * 400으로 내려준다.
 */
@Slf4j
@RestControllerAdvice
public class DeliveryGlobalExceptionHandler {

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handlePropertyReferenceException(PropertyReferenceException e) {
        log.warn("PropertyReferenceException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, "정렬/조회 조건이 올바르지 않습니다: " + e.getPropertyName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, e.getMessage()));
    }
}
