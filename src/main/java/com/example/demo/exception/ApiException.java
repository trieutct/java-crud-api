package com.example.demo.exception;

import org.springframework.http.HttpStatus;

/** Exception gốc của API: mỗi lớp con gắn với một HTTP status, GlobalExceptionHandler xử lý chung */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
