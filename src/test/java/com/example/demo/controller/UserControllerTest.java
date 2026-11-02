package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.UserStatusEnum;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String CREATE_BODY = """
            {"full_name": "Nguyễn An", "email": "an@example.com", "password": "matkhau123", "phone_number": "0901234567"}
            """;
    private static final String UPDATE_BODY = """
            {"full_name": "Nguyễn An", "email": "an@example.com", "phone_number": "0901234567", "status": "INACTIVE"}
            """;
    private static final UserResponse RESPONSE = new UserResponse(
            1, "Nguyễn An", "an@example.com", "0901234567", UserStatusEnum.ACTIVE, Instant.EPOCH, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @Test
    void protectedEndpointsReturn401WithoutToken() throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Thiếu access token"));
        mockMvc.perform(get("/api/users/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void findAllReturns401WhenUserInactive() throws Exception {
        doThrow(new UnauthorizedException("Tài khoản đã bị vô hiệu hóa"))
                .when(authService).authenticate("Bearer abc", false);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị vô hiệu hóa"));
        verifyNoInteractions(userService);
    }

    @Test
    void updateReturns401BeforeValidation() throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDoesNotRequireToken() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(RESPONSE);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated());
        verifyNoInteractions(authService);
    }

    @Test
    void findAllReturnsUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items[0].email").value("an@example.com"))
                .andExpect(jsonPath("$.data.total_items").value(1))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void findByIdReturns400WhenIdInvalid() throws Exception {
        when(userService.findById(0)).thenThrow(new BadRequestException("Id phải lớn hơn 0"));

        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Id phải lớn hơn 0"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(userService.findById(1)).thenThrow(new NotFoundException("Không tìm thấy"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns201WithSnakeCaseFields() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(RESPONSE);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn An"))
                .andExpect(jsonPath("$.data.phone_number").value("0901234567"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.created_at").exists())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist());
    }

    @Test
    void responseNeverContainsPassword() throws Exception {
        when(userService.findById(1)).thenReturn(RESPONSE);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void createReturns400WhenInvalid() throws Exception {
        String body = """
                {"full_name": "", "email": "khong-phai-email", "password": "ngan"}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[?(@.key == 'fullName')].error_code").value(400))
                .andExpect(jsonPath("$.errors[?(@.key == 'email')].message").value("Email không hợp lệ"))
                .andExpect(jsonPath("$.errors[?(@.key == 'password')].message")
                        .value("Mật khẩu phải từ 8 đến 72 ký tự"));
        verifyNoInteractions(userService);
    }

    @Test
    void createReturns400WhenEmailExists() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new BadRequestException("Email đã được sử dụng: an@example.com"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email đã được sử dụng: an@example.com"));
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        when(userService.update(eq(1), any(UpdateUserRequest.class))).thenReturn(RESPONSE);

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn An"));
    }

    @Test
    void updateReturns400WhenStatusUnknown() throws Exception {
        String body = """
                {"full_name": "An", "email": "an@example.com", "status": "KHONG_TON_TAI"}
                """;

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(userService);
    }

    @Test
    void wrongIdTypeUsesCommonFormat() throws Exception {
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedJsonUsesCommonFormat() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void unexpectedErrorHidesInternalDetails() throws Exception {
        when(userService.findById(1)).thenThrow(new IllegalStateException("bí mật nội bộ"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new NotFoundException("Không tìm thấy")).when(userService).delete(1);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNotFound());
    }
}
