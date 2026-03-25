package org.com.repair.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

import org.com.repair.DTO.AuthLoginResponse;
import org.com.repair.entity.AuthRefreshToken;
import org.com.repair.repository.AuthRefreshTokenRepository;
import org.com.repair.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthTokenService {

    private final JwtTokenService jwtTokenService;
    private final AuthRefreshTokenRepository authRefreshTokenRepository;

    public AuthTokenService(JwtTokenService jwtTokenService,
                            AuthRefreshTokenRepository authRefreshTokenRepository) {
        this.jwtTokenService = jwtTokenService;
        this.authRefreshTokenRepository = authRefreshTokenRepository;
    }

    @Transactional
    public <T> AuthLoginResponse<T> issueLoginTokens(Long userId, String username, String role, T user, HttpServletRequest request) {
        authRefreshTokenRepository.revokeAllByUser(userId, role);

        String accessToken = jwtTokenService.createAccessToken(userId, username, role);
        String refreshToken = jwtTokenService.createRefreshToken(userId, username, role);
        persistRefreshToken(refreshToken, userId, username, role, request);

        return new AuthLoginResponse<>(accessToken, refreshToken, role, user);
    }

    @Transactional
    public AuthLoginResponse<Void> refresh(String refreshToken, HttpServletRequest request) {
        Claims claims = jwtTokenService.parseToken(refreshToken);
        String tokenType = String.valueOf(claims.get("type"));
        if (!"refresh".equals(tokenType)) {
            throw new IllegalStateException("非法刷新令牌");
        }

        String jti = String.valueOf(claims.getId());
        AuthRefreshToken stored = authRefreshTokenRepository.findByJtiAndRevokedFalse(jti)
                .orElseThrow(() -> new IllegalStateException("刷新令牌无效或已失效"));

        if (!hash(refreshToken).equals(stored.getTokenHash())) {
            throw new IllegalStateException("刷新令牌校验失败");
        }

        String deviceId = extractDeviceId(request);
        if (stored.getDeviceId() != null && !stored.getDeviceId().equals(deviceId)) {
            throw new IllegalStateException("设备不匹配，刷新请求被拒绝");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            authRefreshTokenRepository.save(stored);
            throw new IllegalStateException("刷新令牌已过期，请重新登录");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String username = String.valueOf(claims.get("username"));
        String role = String.valueOf(claims.get("role"));

        stored.setRevoked(true);
        stored.setLastActiveAt(LocalDateTime.now());
        authRefreshTokenRepository.save(stored);

        String newAccessToken = jwtTokenService.createAccessToken(userId, username, role);
        String newRefreshToken = jwtTokenService.createRefreshToken(userId, username, role);
        persistRefreshToken(newRefreshToken, userId, username, role, request);

        authRefreshTokenRepository.deleteExpired(LocalDateTime.now().minusDays(1));

        return new AuthLoginResponse<>(newAccessToken, newRefreshToken, role, null);
    }

    @Transactional
    public void logout(String refreshToken, HttpServletRequest request) {
        try {
            Claims claims = jwtTokenService.parseToken(refreshToken);
            String jti = claims.getId();
            AuthRefreshToken stored = authRefreshTokenRepository.findByJtiAndRevokedFalse(jti).orElse(null);
            if (stored != null) {
                String deviceId = extractDeviceId(request);
                if (stored.getDeviceId() == null || stored.getDeviceId().equals(deviceId)) {
                    authRefreshTokenRepository.revokeByJti(jti);
                }
            }
        } catch (Exception ignored) {
            // Keep logout idempotent: even invalid token should look like success.
        }
    }

    @Transactional(readOnly = true)
    public List<AuthRefreshToken> listActiveSessions(Long userId, String role) {
        return authRefreshTokenRepository.findByUserIdAndUserRoleAndRevokedFalseOrderByUpdatedAtDesc(userId, role);
    }

    private void persistRefreshToken(String refreshToken, Long userId, String username, String role, HttpServletRequest request) {
        Claims claims = jwtTokenService.parseToken(refreshToken);
        Instant expInstant = claims.getExpiration().toInstant();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expInstant, ZoneId.systemDefault());

        String deviceId = extractDeviceId(request);
        String deviceName = truncate(request.getHeader("X-Device-Name"), 120);
        String userAgent = truncate(request.getHeader("User-Agent"), 255);
        String ipAddress = truncate(extractIp(request), 64);

        AuthRefreshToken entity = AuthRefreshToken.builder()
                .jti(claims.getId())
                .tokenHash(hash(refreshToken))
                .userId(userId)
                .username(username)
                .userRole(role)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        authRefreshTokenRepository.save(entity);
    }

    private String extractDeviceId(HttpServletRequest request) {
        String header = request.getHeader("X-Device-Id");
        if (header == null || header.isBlank()) {
            return "web-default-device";
        }
        return truncate(header.trim(), 120);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] split = forwarded.split(",");
            return split[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("令牌摘要计算失败", ex);
        }
    }
}
