package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.controller.AuthController;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@ExtendWith(OutputCaptureExtension.class)
class ApiLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void logsPayloadAndDataWithSensitiveFieldsMasked(CapturedOutput output) throws Exception {
        when(authService.login(new LoginRequest("a@example.com", "secret-password")))
                .thenReturn(new TokenResponse("secret-access", "secret-refresh"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "a@example.com", "password": "secret-password"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("secret-access"));

        assertThat(output).contains("API vào: POST /api/auth/login")
                .contains("\"email\":\"a@example.com\"")
                .contains("\"password\":\"***\"")
                .contains("API ra: POST /api/auth/login status=200")
                .contains("\"access_token\":\"***\"")
                .contains("\"refresh_token\":\"***\"")
                .doesNotContain("secret-");
    }

    @Test
    void doesNotLogMalformedJson(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"secret-password"))
                .andExpect(status().isBadRequest());

        assertThat(output).contains("status=400").doesNotContain("secret-");
    }

    @Test
    void logsRequestRejectedByAuthInterceptor(CapturedOutput output) throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(put("/api/auth/me/fcm-token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        assertThat(output).contains("API ra: PUT /api/auth/me/fcm-token status=401");
    }
}
