package com.dayaeyak.gateway.util;

import com.dayaeyak.gateway.dto.RequestUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserWebClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<RequestUserInfoDto> getUserInfo(String userId) {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/internal/users/{userId}", userId)
                .retrieve()
                .bodyToMono(RequestUserInfoDto.class);
    }
}
