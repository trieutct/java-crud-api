package com.example.demo.controller;

import com.example.demo.config.RequireAuth;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.NotificationResponse;
import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Gửi thông báo tới user theo FCM token đã lưu */
    @RequireAuth
    @PostMapping
    public ApiResponse<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ApiResponse.success(notificationService.send(request));
    }
}
