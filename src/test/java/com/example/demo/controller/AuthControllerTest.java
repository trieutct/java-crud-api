package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.dto.UpdateFcmTokenRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.UserStatusEnum;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String LOGIN_BODY = """
            {"email": "an@example.com", "password": "matkhau123"}
            """;
    private static final TokenResponse TOKENS = new TokenResponse("access", "refresh");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginReturnsTokensInSnakeCase() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(TOKENS);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").value("access"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh"));
    }

    @Test
    void loginReturns401WhenCredentialsWrong() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Email hoặc mật khẩu không đúng"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void loginReturns400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.key == 'email')].message").value("Email không được để trống"));
        verifyNoInteractions(authService);
    }

    @Test
    void refreshReadsSnakeCaseBody() throws Exception {
        when(authService.refresh(new RefreshTokenRequest("cu"))).thenReturn(TOKENS);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token": "cu"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("access"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        UserResponse user = new UserResponse(
                1, "Nguyễn An", "an@example.com", null, UserStatusEnum.ACTIVE, Instant.EPOCH, null);
        when(authService.me("Bearer abc")).thenReturn(user);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("an@example.com"))
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void meReturns401WhenHeaderMissing() throws Exception {
        when(authService.me(null)).thenThrow(new UnauthorizedException("Thiếu access token"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateFcmTokenReadsSnakeCaseBody() throws Exception {
        UserResponse user = new UserResponse(
                1, "Nguyễn An", "an@example.com", null, UserStatusEnum.ACTIVE, Instant.EPOCH, Instant.EPOCH);
        when(authService.updateFcmToken("Bearer abc", new UpdateFcmTokenRequest("token-1"))).thenReturn(user);

        mockMvc.perform(put("/api/auth/me/fcm-token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcm_token": "token-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("an@example.com"))
                .andExpect(jsonPath("$.data.fcm_token").doesNotExist());
    }

    @Test
    void updateFcmTokenReturns401BeforeValidation() throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(put("/api/auth/me/fcm-token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Thiếu access token"));
    }

    @Test
    void updateFcmTokenReturns400WhenBlank() throws Exception {
        mockMvc.perform(put("/api/auth/me/fcm-token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcm_token": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.key == 'fcmToken')].message").value("FCM token không được để trống"));
    }
}
