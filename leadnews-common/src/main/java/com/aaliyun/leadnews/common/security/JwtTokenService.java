package com.aaliyun.leadnews.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class JwtTokenService {
    private final SecretKey key;
    public JwtTokenService(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String createToken(long userId, String clientType, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder().subject(Long.toString(userId)).claim("userId", userId)
                .claim("clientType", clientType).issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl)))
                .signWith(key).compact();
    }
    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new JwtPrincipal(claims.get("userId", Long.class), claims.get("clientType", String.class));
    }
    public record JwtPrincipal(Long userId, String clientType) {}
}
