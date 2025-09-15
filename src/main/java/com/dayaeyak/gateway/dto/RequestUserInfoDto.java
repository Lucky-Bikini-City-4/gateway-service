package com.dayaeyak.gateway.dto;

import io.jsonwebtoken.Claims;

public record RequestUserInfoDto(
        String userId,

        String role
) {

    public static RequestUserInfoDto from(Claims claims) {
        return new RequestUserInfoDto(
                claims.getSubject(),
                claims.get("role", String.class)
        );
    }
}
