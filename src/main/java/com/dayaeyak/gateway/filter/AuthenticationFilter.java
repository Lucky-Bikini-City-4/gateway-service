package com.dayaeyak.gateway.filter;

import com.dayaeyak.gateway.constraints.HeaderConstraints;
import com.dayaeyak.gateway.dto.RequestUserInfoDto;
import com.dayaeyak.gateway.exception.GatewayException;
import com.dayaeyak.gateway.exception.GatewayExceptionType;
import com.dayaeyak.gateway.util.JwtProvider;
import com.dayaeyak.gateway.util.UserWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;
    private final UserWebClient userWebClient;

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
            return Mono.error(() -> new GatewayException(GatewayExceptionType.INVALID_ACCESS_TOKEN));
        }

        RequestUserInfoDto requestUserInfoDto = jwtProvider.getUserInfoFromToken(token);

        return callUserService(requestUserInfoDto.userId())
                .flatMap(userInfo -> chain.filter(
                        exchange.mutate()
                                .request(builder -> builder
                                        .headers(header -> {
                                            header.set(HeaderConstraints.USER_ID_HEADER, userInfo.userId());
                                            header.set(HeaderConstraints.USER_ROLE_HEADER, userInfo.role());
                                        })
                                )
                                .build()
                ))
                ;
    }

    private Mono<RequestUserInfoDto> callUserService(String userId) {
        return userWebClient.getUserInfo(userId)
                .doOnSubscribe(s -> log.debug("유저 정보 조회 요청 {}", userId))
                .doOnNext(u -> log.debug("유저 정보 조회 성공 {}", userId))
                .doOnError(e -> log.debug("유저 정보 조회 실패 {}", userId));
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

    @Override
    public int getOrder() {
        return 1;
    }
}
