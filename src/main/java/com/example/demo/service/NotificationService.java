package com.example.demo.service;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.exception.BadGatewayException;
import com.example.demo.exception.BadRequestException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final UserService userService;

    public NotificationService(FirebaseMessaging firebaseMessaging, UserService userService) {
        this.firebaseMessaging = firebaseMessaging;
        this.userService = userService;
    }

    /**
     * Gửi thông báo tới user theo FCM token, báo lỗi 404 nếu không có user, 400 nếu user chưa có token hoặc
     * token không dùng được, 502 nếu FCM lỗi. Không mở transaction để không giữ kết nối database khi gọi FCM.
     */
    public NotificationResponse send(SendNotificationRequest request) {
        String fcmToken = userService.getFcmToken(request.userId());
        Message message = Message.builder()
                // setToken đã deprecated, nhưng setFid nhận Installation ID chứ không nhận token
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.body())
                        .build())
                .build();

        try {
            return new NotificationResponse(firebaseMessaging.send(message));
        } catch (FirebaseMessagingException ex) {
            log.warn("Gửi FCM thất bại: userId={}, errorCode={}", request.userId(), ex.getMessagingErrorCode(), ex);
            if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                userService.clearFcmToken(request.userId(), fcmToken);
                throw new BadRequestException("FCM token của user đã hết hiệu lực");
            }
            if (ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                throw new BadRequestException("FCM từ chối token hoặc nội dung thông báo");
            }
            throw new BadGatewayException("Không gửi được thông báo tới FCM");
        }
    }
}
