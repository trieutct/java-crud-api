package com.example.demo.dto;

/** Kết quả gửi thông báo, messageId là mã do FCM cấp */
public record NotificationResponse(String messageId) {
}
