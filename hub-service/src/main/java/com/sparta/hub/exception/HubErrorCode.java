package com.sparta.hub.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCodeIfs{

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "존재하지 않는 허브입니다."),


    HUB_ALREADY_EXISTS(HttpStatus.CONFLICT, "H002", "이미 등록된 허브입니다."),


    HUB_ALREADY_DELETED(HttpStatus.CONFLICT, "H003", "이미 삭제된 허브입니다."),

    HUB_IN_USE(HttpStatus.CONFLICT, "H004", "현재 배송에서 사용 중인 허브이므로 삭제할 수 없습니다."),


    HUB_INACTIVE(HttpStatus.CONFLICT, "H005", "비활성화된 허브입니다."),

    HUB_CANNOT_RECEIVE_NEW_DELIVERY(HttpStatus.CONFLICT, "H006", "신규 배송을 받을 수 없는 허브입니다."),


    INVALID_HUB_NAME(HttpStatus.BAD_REQUEST, "H007", "허브 이름이 올바르지 않습니다."),

    INVALID_HUB_ADDRESS(HttpStatus.BAD_REQUEST, "H008", "허브 주소가 올바르지 않습니다."),

    INVALID_LATITUDE(HttpStatus.BAD_REQUEST, "H009", "위도는 -90 이상 90 이하여야 합니다."),

    INVALID_LONGITUDE(HttpStatus.BAD_REQUEST, "H010", "경도는 -180 이상 180 이하여야 합니다.");








    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


}
