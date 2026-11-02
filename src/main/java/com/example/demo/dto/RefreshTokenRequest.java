package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/** Dữ liệu client gửi lên khi xin cấp lại token */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken) {
}
