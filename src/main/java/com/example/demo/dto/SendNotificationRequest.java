package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dữ liệu gửi thông báo, giới hạn độ dài để không vượt 4KB payload của FCM khi viết tiếng Việt có dấu */
public record SendNotificationRequest(
        @NotNull(message = "User nhận thông báo không được để trống")
        Integer userId,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
        String title,

        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự")
        String body) {
}
