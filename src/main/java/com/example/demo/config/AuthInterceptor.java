package com.example.demo.config;

import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    /** Kiểm tra token của API có @RequireAuth, báo lỗi 401 nếu token không hợp lệ hoặc user không được phép gọi */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequireAuth requireAuth = method.getMethodAnnotation(RequireAuth.class);
            if (requireAuth != null) {
                authService.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION), requireAuth.allowInactive());
            }
        }
        return true;
    }
}
