package com.example.toyguard.config;

import com.example.toyguard.model.AppUser;
import com.example.toyguard.model.Role;
import com.example.toyguard.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER = "currentUser";
    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || requestUri.contains("/auth/login")
                || requestUri.contains("/public/") || requestUri.contains("/swagger")
                || requestUri.contains("/v3/api-docs")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        Optional<AppUser> user = tokenService.verify(authHeader);

        if (user.isEmpty()) {
            Optional<AppUser> disabledUser = tokenService.verifyIgnoreDisabled(authHeader);
            if (disabledUser.isPresent() && RolePermissionConfig.isSensitiveOperation(requestUri)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"身份已失效，无权执行敏感操作\"}");
                return false;
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        AppUser currentUser = user.get();
        Set<Role> allowedRoles = RolePermissionConfig.getAllowedRoles(requestUri);
        if (allowedRoles != null && !allowedRoles.contains(currentUser.role())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"无权限访问该资源\"}");
            return false;
        }

        request.setAttribute(CURRENT_USER, currentUser);
        return true;
    }
}
