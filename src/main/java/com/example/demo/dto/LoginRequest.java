package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/** Dữ liệu client gửi lên khi đăng nhập */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password) {
}
