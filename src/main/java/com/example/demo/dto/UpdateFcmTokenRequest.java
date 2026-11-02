package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** FCM token do thiết bị của user đang đăng nhập gửi lên */
public record UpdateFcmTokenRequest(
        @NotBlank(message = "FCM token không được để trống")
        @Size(max = 512, message = "FCM token tối đa 512 ký tự")
        String fcmToken) {
}
