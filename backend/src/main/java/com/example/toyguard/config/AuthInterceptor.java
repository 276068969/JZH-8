package com.example.toyguard.config;

import com.example.toyguard.model.AppUser;
import com.example.toyguard.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER = "currentUser";
    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().contains("/auth/login")
                || request.getRequestURI().contains("/public/") || request.getRequestURI().contains("/swagger")
                || request.getRequestURI().contains("/v3/api-docs")) {
            return true;
        }
        Optional<AppUser> user = tokenService.verify(request.getHeader("Authorization"));
        if (user.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        request.setAttribute(CURRENT_USER, user.get());
        return true;
    }
}
