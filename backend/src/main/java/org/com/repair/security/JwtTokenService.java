package org.com.repair.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtTokenService(@Value("${security.jwt.secret}") String secret,
                           @Value("${security.jwt.access-expiration-seconds:3600}") long accessExpirationSeconds,
                           @Value("${security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String createAccessToken(Long userId, String username, String role) {
        return createToken(userId, username, role, "access", accessExpirationSeconds);
    }

    public String createRefreshToken(Long userId, String username, String role) {
        return createToken(userId, username, role, "refresh", refreshExpirationSeconds);
    }

    private String createToken(Long userId, String username, String role, String type, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
