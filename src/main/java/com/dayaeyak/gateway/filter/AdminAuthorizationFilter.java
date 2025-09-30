package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.enums.UserRole;
import com.dayaeyak.gateway.exception.GatewayException;
import com.dayaeyak.gateway.exception.GatewayExceptionType;
import com.dayaeyak.gateway.util.LogUtil;
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

import java.util.List;

@Component
public class AdminAuthorizationFilter implements GlobalFilter, Ordered {

    private final List<PathPattern> adminOnlyPathList = List.of(
            new PathPatternParser().parse("/admin/**"),
            new PathPatternParser().parse("/backoffice/**")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String filter = this.getClass().getSimpleName();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        LogUtil.info(method, path, filter, "start");

        if (!isAdminOnlyPath(path)) {
            return chain.filter(exchange);
        }

        String role = request.getHeaders().getFirst(HeaderConstraints.USER_ROLE_HEADER);

        if (role == null || !UserRole.MASTER.equals(UserRole.of(role))) {
            LogUtil.warn(method, path, filter, "forbidden");
            return Mono.error(() -> new GatewayException(GatewayExceptionType.FORBIDDEN));
        }

        return chain.filter(exchange);
    }

    private boolean isAdminOnlyPath(String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        return adminOnlyPathList.stream().anyMatch(white -> white.matches(pathContainer));
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
