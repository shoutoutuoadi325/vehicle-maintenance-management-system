package org.com.repair.config;

import org.com.repair.security.JwtAuthenticationInterceptor;
import org.com.repair.security.OwnershipAuthorizationInterceptor;
import org.com.repair.security.RoleAuthorizationInterceptor;
import org.com.repair.security.SecurityAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthInterceptorConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;
    private final OwnershipAuthorizationInterceptor ownershipAuthorizationInterceptor;
    private final SecurityAuditInterceptor securityAuditInterceptor;

    public AuthInterceptorConfig(JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
                                 RoleAuthorizationInterceptor roleAuthorizationInterceptor,
                                 OwnershipAuthorizationInterceptor ownershipAuthorizationInterceptor,
                                 SecurityAuditInterceptor securityAuditInterceptor) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.roleAuthorizationInterceptor = roleAuthorizationInterceptor;
        this.ownershipAuthorizationInterceptor = ownershipAuthorizationInterceptor;
        this.securityAuditInterceptor = securityAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityAuditInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/users/login",
                "/api/technicians/login",
                "/api/admins/login",
                "/api/users",
                "/api/technicians",
                "/api/admins",
                "/api/ai-diagnosis/**",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html");

        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/users/login",
                        "/api/technicians/login",
                        "/api/admins/login",
                        "/api/users",
                        "/api/technicians",
                        "/api/admins",
                        "/api/ai-diagnosis/**",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");

                registry.addInterceptor(ownershipAuthorizationInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(
                        "/api/users/login",
                        "/api/technicians/login",
                        "/api/admins/login",
                        "/api/users",
                        "/api/technicians",
                        "/api/admins",
                        "/api/ai-diagnosis/**",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");
    }
}
