package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.exception.GatewayException;
import com.dayaeyak.gateway.exception.ExceptionResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionHandlingFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> handlingError(exchange, throwable));
    }

    private Mono<Void> handlingError(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        ExceptionResponseDto responseDto = buildExceptionResponse(exchange, throwable);

        log.warn(responseDto.toString());

        HttpStatus status = responseDto.status();
        response.setStatusCode(status);

        response.getHeaders().add(HeaderConstraints.RESPONSE_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return writeErrorResponse(response, responseDto);
    }

    private ExceptionResponseDto buildExceptionResponse(ServerWebExchange exchange, Throwable throwable) {
        if (throwable instanceof GatewayException gatewayException) {
            return ExceptionResponseDto.builder()
                    .status(gatewayException.getStatus())
                    .message(gatewayException.getMessage())
                    .method(exchange.getRequest().getMethod().name())
                    .requestPath(exchange.getRequest().getPath().value())
                    .build();
        }

        return ExceptionResponseDto.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Unknown Internal Server Error")
                .method(exchange.getRequest().getMethod().name())
                .requestPath(exchange.getRequest().getPath().value())
                .build();
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, ExceptionResponseDto responseDto) {
        try {
            String errorJson = objectMapper.writeValueAsString(responseDto);

            DataBufferFactory bufferFactory = response.bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(errorJson.getBytes(StandardCharsets.UTF_8));

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
