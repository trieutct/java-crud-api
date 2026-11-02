package com.example.demo.config;

import com.example.demo.service.ImageService;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor, @Value("${upload.dir}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
    }

    /** Phục vụ file ảnh đã upload, không yêu cầu token để thẻ img của trình duyệt tải được */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Thư mục chưa tồn tại thì URI không có dấu / ở cuối, mà Spring cần dấu này để tìm file bên trong
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(ImageService.PUBLIC_PATH + "**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    /** Kiểm tra xác thực cho các API có @RequireAuth */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
    }

    /** Thêm tiền tố /api cho mọi @RestController */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
