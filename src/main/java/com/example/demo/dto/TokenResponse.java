package com.example.demo.dto;

/** Cặp token trả về khi đăng nhập hoặc refresh */
public record TokenResponse(String accessToken, String refreshToken) {
}
