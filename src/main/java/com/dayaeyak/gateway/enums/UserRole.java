package com.dayaeyak.gateway.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    MASTER("MASTER"),
    SELLER("SELLER"),
    NORMAL("NORMAL"),
    ;

    private final String role;

    public static UserRole of(String role) {
        return Stream.of(UserRole.values())
                .filter(userRole -> userRole.role.equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(RuntimeException::new);
    }
}
