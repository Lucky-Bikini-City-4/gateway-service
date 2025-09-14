package com.dayaeyak.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GatewayException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public GatewayException(GatewayExceptionType e) {
        super(e.getMessage());

        this.status = e.getStatus();
        this.message = e.getMessage();
    }
}
