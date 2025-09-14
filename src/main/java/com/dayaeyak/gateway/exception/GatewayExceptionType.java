package com.dayaeyak.gateway.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GatewayExceptionType {

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청에 대한 권한이 부족합니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
