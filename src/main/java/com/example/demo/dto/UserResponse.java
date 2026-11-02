package com.example.demo.dto;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatusEnum;
import java.time.Instant;

/** Dữ liệu trả về cho client, tách khỏi Entity để không lộ passwordHash và cấu trúc database */
public record UserResponse(
        Integer id,
        String fullName,
        String email,
        String phoneNumber,
        UserStatusEnum status,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
