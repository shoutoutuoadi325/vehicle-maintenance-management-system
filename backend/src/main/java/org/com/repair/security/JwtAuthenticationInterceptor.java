package org.com.repair.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.jsonwebtoken.Claims;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "缺少或非法 Authorization 头");
            return false;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            Claims claims = jwtTokenService.parseToken(token);
            String tokenType = String.valueOf(claims.get("type"));
            if (!"access".equals(tokenType)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "访问令牌类型非法");
                return false;
            }

            Long userId = Long.parseLong(claims.getSubject());
            String role = String.valueOf(claims.get("role"));

            request.setAttribute("authUserId", userId);
            request.setAttribute("authRole", role.toLowerCase());
            return true;
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录态失效，请重新登录");
            return false;
        }
    }
}
