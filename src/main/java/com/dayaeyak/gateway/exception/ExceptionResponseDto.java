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
}
