package com.example.demo.dto;

import com.example.demo.entity.Image;
import java.time.Instant;

/** Thông tin ảnh trả về cho client, filePath là đường dẫn tương đối, url là đường dẫn đầy đủ do controller gán */
public record ImageResponse(
        Integer id,
        String originalName,
        String filePath,
        String url,
        String contentType,
        Long fileSize,
        Instant createdAt) {

    public static ImageResponse from(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getOriginalName(),
                image.getFilePath(),
                null,
                image.getContentType(),
                image.getFileSize(),
                image.getCreatedAt());
    }

    public ImageResponse withUrl(String url) {
        return new ImageResponse(id, originalName, filePath, url, contentType, fileSize, createdAt);
    }
}
