package org.com.repair.controller;

import java.util.List;
import java.util.Map;

import org.com.repair.DTO.AuthLoginResponse;
import org.com.repair.DTO.AuthLogoutRequest;
import org.com.repair.DTO.AuthRefreshRequest;
import org.com.repair.DTO.AuthSessionResponse;
import org.com.repair.entity.AuthRefreshToken;
import org.com.repair.service.AuthTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthTokenService authTokenService;

    public AuthController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AuthSessionResponse>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        String role = String.valueOf(request.getAttribute("authRole"));

        List<AuthSessionResponse> sessions = authTokenService.listActiveSessions(userId, role)
                .stream()
                .map(this::toSessionResponse)
                .toList();
        return new ResponseEntity<>(sessions, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthLoginResponse<Void>> refresh(@Valid @RequestBody AuthRefreshRequest request,
                                                           HttpServletRequest servletRequest) {
        AuthLoginResponse<Void> refreshed = authTokenService.refresh(request.refreshToken(), servletRequest);
        return new ResponseEntity<>(refreshed, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody AuthLogoutRequest request,
                                                      HttpServletRequest servletRequest) {
        authTokenService.logout(request.refreshToken(), servletRequest);
        return new ResponseEntity<>(Map.of("message", "已退出登录"), HttpStatus.OK);
    }

    private AuthSessionResponse toSessionResponse(AuthRefreshToken token) {
        return new AuthSessionResponse(
                token.getDeviceId(),
                token.getDeviceName(),
                token.getIpAddress(),
                token.getLastActiveAt(),
                token.getExpiresAt());
    }
}
