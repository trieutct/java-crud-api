package com.example.demo.dto;

/** Chi tiết 1 lỗi cụ thể trong response lỗi (ví dụ lỗi từng field khi validate) */
public record ErrorDetail(String key, int errorCode, String message) {
}
