package org.com.repair.config;

import java.util.List;
import java.util.Objects;

import org.com.repair.security.JwtAuthenticationInterceptor;
import org.com.repair.security.OwnershipAuthorizationInterceptor;
import org.com.repair.security.RoleAuthorizationInterceptor;
import org.com.repair.security.SecurityAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthInterceptorConfig implements WebMvcConfigurer {

    private static final List<String> AUTH_EXCLUDE_PATHS = List.of(
            "/api/users/login",
            "/api/technicians/login",
            "/api/admins/login",
            "/api/users",
            "/api/technicians",
            "/api/admins",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

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
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(securityAuditInterceptor))
                .addPathPatterns("/api/**");

        registry.addInterceptor(Objects.requireNonNull(jwtAuthenticationInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(AUTH_EXCLUDE_PATHS);

        registry.addInterceptor(Objects.requireNonNull(roleAuthorizationInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(AUTH_EXCLUDE_PATHS);

        registry.addInterceptor(Objects.requireNonNull(ownershipAuthorizationInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(AUTH_EXCLUDE_PATHS);
    }
}
