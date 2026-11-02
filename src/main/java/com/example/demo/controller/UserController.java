package com.example.demo.controller;

import com.example.demo.config.RequireAuth;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.ListResponse;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Lấy danh sách tất cả user */
    @RequireAuth(allowInactive = true)
    @GetMapping
    public ApiResponse<ListResponse<UserResponse>> findAll() {
        return ApiResponse.successList(userService.findAll());
    }

    /** Lấy một user theo id */
    @RequireAuth
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Integer id) {
        return ApiResponse.success(userService.findById(id));
    }

    /** Tạo user mới, trả về 201 kèm đường dẫn của user vừa tạo */
    @RequireAuth
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        UserResponse created = userService.create(request);
        URI location = URI.create(httpRequest.getRequestURI() + "/" + created.id());
        return ResponseEntity.created(location).body(ApiResponse.created(created));
    }

    /** Cập nhật thông tin user theo id */
    @RequireAuth
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /** Xóa user theo id, trả về 204 khi thành công */
    @RequireAuth
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
