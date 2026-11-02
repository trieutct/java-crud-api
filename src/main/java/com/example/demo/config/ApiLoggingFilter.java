package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Giữ lại bản sao body của request và response để ApiLoggingInterceptor đọc được, vì stream gốc chỉ đọc được một lần */
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    static final int REQUEST_CACHE_LIMIT = 64 * 1024;

    /** Không bọc file ảnh ở /uploads để tránh giữ cả file trong bộ nhớ */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    /** Bọc request, response và trả lại body đã giữ cho client sau khi xử lý xong */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT), wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
}
