package com.example.demo.dto;

import java.util.List;

/** Shape dữ liệu dạng danh sách kèm tổng số bản ghi */
public record ListResponse<T>(List<T> items, long totalItems) {
}
