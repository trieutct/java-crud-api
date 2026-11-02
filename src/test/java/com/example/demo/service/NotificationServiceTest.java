package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.exception.BadGatewayException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final SendNotificationRequest REQUEST = new SendNotificationRequest(1, "Tiêu đề", "Nội dung");

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    private FirebaseMessagingException fcmError(MessagingErrorCode errorCode) {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(errorCode);
        return error;
    }

    @Test
    void sendUsesStoredTokenAndReturnsMessageId() throws Exception {
        when(userService.getFcmToken(1)).thenReturn("token-1");
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/demo/messages/1");

        NotificationResponse response = notificationService.send(REQUEST);

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(sent.capture());
        Object notification = ReflectionTestUtils.getField(sent.getValue(), "notification");
        assertThat(ReflectionTestUtils.getField(sent.getValue(), "token")).isEqualTo("token-1");
        assertThat(ReflectionTestUtils.getField(notification, "title")).isEqualTo("Tiêu đề");
        assertThat(ReflectionTestUtils.getField(notification, "body")).isEqualTo("Nội dung");
        assertThat(response.messageId()).isEqualTo("projects/demo/messages/1");
    }

    @Test
    void sendDoesNotCallFcmWhenUserHasNoToken() {
        when(userService.getFcmToken(1)).thenThrow(new BadRequestException("User chưa đăng ký FCM token"));

        assertThatThrownBy(() -> notificationService.send(REQUEST))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    void sendDoesNotCallFcmWhenUserMissing() {
        when(userService.getFcmToken(1)).thenThrow(new NotFoundException("Không tìm thấy"));

        assertThatThrownBy(() -> notificationService.send(REQUEST))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    void sendClearsTokenWhenUnregistered() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.UNREGISTERED);
        when(userService.getFcmToken(1)).thenReturn("token-1");
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        assertThatThrownBy(() -> notificationService.send(REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("FCM token của user đã hết hiệu lực");
        verify(userService).clearFcmToken(1, "token-1");
    }

    @Test
    void sendReturns400AndKeepsTokenWhenFcmRejectsArgument() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.INVALID_ARGUMENT);
        when(userService.getFcmToken(1)).thenReturn("token-1");
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        assertThatThrownBy(() -> notificationService.send(REQUEST))
                .isInstanceOf(BadRequestException.class);
        verify(userService, never()).clearFcmToken(any(), any());
    }

    @Test
    void sendReturns502AndKeepsTokenWhenFcmUnavailable() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.UNAVAILABLE);
        when(userService.getFcmToken(1)).thenReturn("token-1");
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        assertThatThrownBy(() -> notificationService.send(REQUEST))
                .isInstanceOf(BadGatewayException.class);
        verify(userService, never()).clearFcmToken(any(), any());
    }
}
