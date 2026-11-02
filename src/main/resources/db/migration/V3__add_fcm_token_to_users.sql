-- FCM token của thiết bị, null khi user chưa đăng ký nhận thông báo
ALTER TABLE users
    ADD COLUMN fcm_token VARCHAR(512);
