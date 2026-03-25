package org.com.repair.security;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private static final Set<String> ADMIN_ONLY_PREFIXES = Set.of(
            "/api/admins"
    );

    private static final Set<String> CUSTOMER_ONLY_PREFIXES = Set.of(
            "/api/gamification"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String role = String.valueOf(request.getAttribute("authRole")).toLowerCase();

        if (isOptions(request)) {
            return true;
        }

        if (isPrefixMatched(path, ADMIN_ONLY_PREFIXES) && !"admin".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "仅管理员可访问该接口");
            return false;
        }

        if (isPrefixMatched(path, CUSTOMER_ONLY_PREFIXES) && !"customer".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "仅顾客可访问该接口");
            return false;
        }

        // Technician APIs are writable by technicians and admins.
        if (path.startsWith("/api/technicians") && isWriteMethod(request)) {
            if (!"technician".equals(role) && !"admin".equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "仅技师或管理员可修改技师相关资源");
                return false;
            }
        }

        // Material management is treated as backoffice operation.
        if (path.startsWith("/api/material") && isWriteMethod(request)) {
            if (!"technician".equals(role) && !"admin".equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "仅技师或管理员可修改物料资源");
                return false;
            }
        }

        return true;
    }

    private boolean isPrefixMatched(String path, Set<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }

    private boolean isOptions(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private boolean isWriteMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }
}
