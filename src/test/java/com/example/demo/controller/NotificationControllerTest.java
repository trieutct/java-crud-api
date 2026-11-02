package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.exception.BadGatewayException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final String BODY = """
            {"user_id": 1, "title": "Tiêu đề", "body": "Nội dung"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AuthService authService;

    @Test
    void sendReturnsMessageIdInSnakeCase() throws Exception {
        when(notificationService.send(new SendNotificationRequest(1, "Tiêu đề", "Nội dung")))
                .thenReturn(new NotificationResponse("projects/demo/messages/1"));

        mockMvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message_id").value("projects/demo/messages/1"));
    }

    @Test
    void sendReturns401WithoutToken() throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    @Test
    void sendReturns400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.key == 'userId')].message")
                        .value("User nhận thông báo không được để trống"))
                .andExpect(jsonPath("$.errors[?(@.key == 'title')].message").value("Tiêu đề không được để trống"))
                .andExpect(jsonPath("$.errors[?(@.key == 'body')].message").value("Nội dung không được để trống"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void sendReturns502WhenFcmFails() throws Exception {
        when(notificationService.send(any(SendNotificationRequest.class)))
                .thenThrow(new BadGatewayException("Không gửi được thông báo tới FCM"));

        mockMvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("Không gửi được thông báo tới FCM"));
    }
}
