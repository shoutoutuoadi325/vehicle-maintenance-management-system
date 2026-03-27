package org.com.repair.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestUserContextResolver {

    public Long requireUserId(HttpServletRequest request) {
        Object value = request.getAttribute("authUserId");
        if (value == null) {
            throw new IllegalStateException("缺少用户身份，请重新登录");
        }
        if (value instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("非法用户身份");
    }

    public String requireRole(HttpServletRequest request) {
        Object value = request.getAttribute("authRole");
        if (value == null) {
            throw new IllegalStateException("缺少用户角色，请重新登录");
        }
        return String.valueOf(value).trim().toLowerCase();
    }

    public void requireCustomerRole(HttpServletRequest request) {
        String role = requireRole(request);
        if (!"customer".equals(role)) {
            throw new IllegalStateException("仅顾客允许访问零碳公路功能");
        }
    }

    public void requireAdminRole(HttpServletRequest request) {
        String role = requireRole(request);
        if (!"admin".equals(role)) {
            throw new IllegalStateException("仅管理员允许访问该功能");
        }
    }

    public void ensurePathUserMatch(HttpServletRequest request, Long userIdInPath) {
        Long actualUserId = requireUserId(request);
        if (!actualUserId.equals(userIdInPath)) {
            throw new IllegalStateException("身份不匹配，禁止越权访问");
        }
    }
}
