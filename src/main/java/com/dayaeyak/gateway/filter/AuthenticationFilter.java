package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.dto.RequestUserInfo;
import com.dayaeyak.gateway.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter {

    private final JwtProvider jwtProvider;

    // white list
    private final List<PathPattern> whiteList = List.of(
            new PathPatternParser().parse("/auth/**")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);

        if (!StringUtils.hasText(token) || !jwtProvider.validateAccessToken(token)) {
            log.warn("Invalid access token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        RequestUserInfo userInfo = jwtProvider.getUserInfoFromToken(token);

        return chain.filter(
                exchange.mutate()
                        .request(builder -> builder
                                .headers(header -> {
                                    header.set(HeaderConstraints.USER_ID_HEADER, userInfo.userId());
                                    header.set(HeaderConstraints.USER_ROLE_HEADER, userInfo.role());
                                })
                        )
                        .build()
        );
    }

    private boolean isWhiteListed(String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        return whiteList.stream().anyMatch(white -> white.matches(pathContainer));
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HeaderConstraints.AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(HeaderConstraints.BEARER_PREFIX)) {
            return authHeader.substring(7);
        }

        return null;
    }
}
