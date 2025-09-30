package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.exception.ExceptionResponseDto;
import com.dayaeyak.gateway.exception.GatewayException;
import com.dayaeyak.gateway.util.LogUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

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
        String filter = this.getClass().getSimpleName();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        LogUtil.warn(method, path, filter, "start");

        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        ExceptionResponseDto responseDto = buildExceptionResponse(method, path, throwable);

        LogUtil.warn(method, path, filter, responseDto.status().name(), responseDto.message());

        response.setStatusCode(responseDto.status());
        response.getHeaders().add(HeaderConstraints.RESPONSE_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return writeErrorResponse(response, responseDto);
    }

    private ExceptionResponseDto buildExceptionResponse(String method, String path, Throwable throwable) {
        if (throwable instanceof GatewayException gatewayException) {
            return ExceptionResponseDto.builder()
                    .status(gatewayException.getStatus())
                    .message(gatewayException.getMessage())
                    .method(method)
                    .requestPath(path)
                    .build();
        }

        return ExceptionResponseDto.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(throwable.getMessage())
                .method(method)
                .requestPath(path)
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
