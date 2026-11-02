package com.example.demo.controller;

import com.example.demo.config.RequireAuth;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ImageResponse;
import com.example.demo.dto.ListResponse;
import com.example.demo.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /** Lấy thông tin tất cả ảnh */
    @RequireAuth
    @GetMapping
    public ApiResponse<ListResponse<ImageResponse>> findAll() {
        return ApiResponse.successList(imageService.findAll().stream().map(this::withUrl).toList());
    }

    /** Lấy thông tin một ảnh theo id */
    @RequireAuth
    @GetMapping("/{id}")
    public ApiResponse<ImageResponse> findById(@PathVariable Integer id) {
        return ApiResponse.success(withUrl(imageService.findById(id)));
    }

    /** Upload ảnh qua form-data với trường file, trả về 201 kèm đường dẫn của bản ghi vừa tạo */
    @RequireAuth
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse>> create(
            @RequestParam MultipartFile file, HttpServletRequest httpRequest) {
        log.info("Upload ảnh - đầu vào: name={}, contentType={}, size={}",
                sanitize(file.getOriginalFilename()), sanitize(file.getContentType()), file.getSize());
        ImageResponse created = withUrl(imageService.create(file));
        log.info("Upload ảnh - đầu ra: {}", sanitize(created));
        URI location = URI.create(httpRequest.getRequestURI() + "/" + created.id());
        return ResponseEntity.created(location).body(ApiResponse.created(created));
    }

    /** Ghép scheme và host của request với filePath để ra đường dẫn đầy đủ */
    private ImageResponse withUrl(ImageResponse image) {
        return image.withUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path(image.filePath()).toUriString());
    }

    /** Thay ký tự xuống dòng để dữ liệu từ client không giả mạo được dòng log */
    private String sanitize(Object value) {
        return String.valueOf(value).replaceAll("[\\r\\n]", "_");
    }
}
