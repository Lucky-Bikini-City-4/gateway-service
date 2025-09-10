package com.dayaeyak.gateway.dto;

import io.jsonwebtoken.Claims;

public record RequestUserInfo(
        String userId,

        String role
) {

    public static RequestUserInfo from(Claims claims) {
        return new RequestUserInfo(
                claims.getSubject(),
                claims.get("role", String.class)
        );
    }
}
