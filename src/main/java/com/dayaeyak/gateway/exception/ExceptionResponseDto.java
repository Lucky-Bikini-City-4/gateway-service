package com.dayaeyak.gateway.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ExceptionResponseDto(
        HttpStatus status,

        String message,

        String method,

        String requestPath
) {

    @Override
    public String toString() {
        return String.format("[%s %s][%s] %s", method, requestPath, status, message);

    }
}
