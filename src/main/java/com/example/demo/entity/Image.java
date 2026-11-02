package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Thông tin ảnh đã upload, không có setter vì ảnh không được sửa sau khi tạo */
@Entity
@Table(name = "images")
public class Image extends BaseEntity {

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, unique = true)
    private String filePath;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    protected Image() {
    }

    public Image(String originalName, String filePath, String contentType, Long fileSize) {
        this.originalName = originalName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }
}
