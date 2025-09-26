package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.enums.UserRole;
import com.dayaeyak.gateway.exception.GatewayException;
import com.dayaeyak.gateway.exception.GatewayExceptionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AdminAuthorizationFilter implements GlobalFilter, Ordered {

    private final PathPattern adminPath = new PathPatternParser().parse("/admin/**");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        PathContainer pathContainer = PathContainer.parsePath(path);

        if (!adminPath.matches(pathContainer)) {
            return chain.filter(exchange);
        }

        String role = request.getHeaders().getFirst(HeaderConstraints.USER_ROLE_HEADER);

        if (role == null || !UserRole.MASTER.equals(UserRole.of(role))) {
            return Mono.error(() -> new GatewayException(GatewayExceptionType.FORBIDDEN));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
