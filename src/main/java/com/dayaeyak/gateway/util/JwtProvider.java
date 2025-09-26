package com.dayaeyak.gateway.util;

import com.dayaeyak.gateway.dto.RequestUserInfoDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey accessSecretKey;

    public JwtProvider(
            @Value("${jwt.access.key}") String accessKey
    ) {
        this.accessSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessKey));
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token, accessSecretKey);
            return claims.getExpiration().after(new Date(System.currentTimeMillis()));
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Empty or null JWT: {}", e.getMessage());
        }

        return false;
    }

    public RequestUserInfoDto getUserInfoFromToken(String token) {
        Claims claims = parseClaims(token, accessSecretKey);

        return RequestUserInfoDto.from(claims);
    }

    private Claims parseClaims(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
