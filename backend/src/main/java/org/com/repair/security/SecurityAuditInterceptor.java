package org.com.repair.security;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityAuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditInterceptor.class);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        request.setAttribute("auditStartNs", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        Object start = request.getAttribute("auditStartNs");
        long durationMs = 0L;
        if (start instanceof Long startNs) {
            durationMs = (System.nanoTime() - startNs) / 1_000_000;
        }

        String requestId = String.valueOf(request.getAttribute("requestId"));
        Object userId = request.getAttribute("authUserId");
        Object role = request.getAttribute("authRole");

        logger.info("security_audit requestId={} method={} path={} status={} userId={} role={} durationMs={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                userId,
                role,
                durationMs);

        if (ex != null) {
            logger.warn("security_audit_error requestId={} message={}", requestId, ex.getMessage());
        }
    }
}
