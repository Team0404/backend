package com.sparta.hub.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubRouteErrorCode implements ErrorCodeIfs {


    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HR001", "존재하지 않는 허브 이동 경로입니다."),


    DIRECT_HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HR002", "출발 허브와 도착 허브 사이에 등록된 직접 이동 경로가 없습니다."),


    HUB_ROUTE_PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "HR003", "출발 허브에서 도착 허브까지 이동할 수 있는 경로가 없습니다."),


    HUB_ROUTE_ALREADY_EXISTS(HttpStatus.CONFLICT, "HR004", "이미 등록된 허브 이동 경로입니다."),

    SAME_DEPARTURE_AND_ARRIVAL_HUB(HttpStatus.BAD_REQUEST, "HR005", "출발 허브와 도착 허브는 같을 수 없습니다."),


    HUB_ROUTE_ALREADY_DELETED(HttpStatus.CONFLICT, "HR006", "이미 삭제된 허브 이동 경로입니다."),


    INVALID_DURATION_MINUTES(HttpStatus.BAD_REQUEST, "HR007", "예상 소요 시간은 0보다 커야 합니다."),

    INVALID_DISTANCE(HttpStatus.BAD_REQUEST, "HR008", "이동 거리는 0보다 커야 합니다."),


    DEPARTURE_HUB_INACTIVE(HttpStatus.CONFLICT, "HR009", "출발 허브가 비활성 상태이므로 이동 경로를 사용할 수 없습니다."),

    ARRIVAL_HUB_INACTIVE(HttpStatus.CONFLICT, "HR010", "도착 허브가 비활성 상태이므로 이동 경로를 사용할 수 없습니다.");



    private final HttpStatus httpStatus;
    private final String code;
    private final String message;



}
