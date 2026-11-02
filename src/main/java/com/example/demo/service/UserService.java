package com.example.demo.service;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /** BCrypt chỉ nhận tối đa 72 byte, ký tự tiếng Việt có dấu chiếm nhiều byte hơn 1 */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lấy tất cả user và chuyển sang DTO */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Lấy một user theo id */
    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return UserResponse.from(getUser(id));
    }

    /** Tạo user mới và băm mật khẩu, báo lỗi 400 nếu mật khẩu quá dài hoặc email đã tồn tại */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BadRequestException("Mật khẩu tối đa 72 byte");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email đã được sử dụng: " + request.email());
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        return UserResponse.from(userRepository.save(user));
    }

    /** Cập nhật user, báo lỗi 400 nếu email đang thuộc về user khác */
    @Transactional
    public UserResponse update(Integer id, UpdateUserRequest request) {
        User user = getUser(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new BadRequestException("Email đã được sử dụng: " + request.email());
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setStatus(request.status());
        // Flush ngay để updatedAt trong response là giá trị mới
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /** Xóa user theo id */
    @Transactional
    public void delete(Integer id) {
        userRepository.delete(getUser(id));
    }

    /** Lấy FCM token của user, báo lỗi 400 nếu user chưa đăng ký token */
    @Transactional(readOnly = true)
    public String getFcmToken(Integer id) {
        String fcmToken = getUser(id).getFcmToken();
        if (fcmToken == null) {
            throw new BadRequestException("User chưa đăng ký FCM token");
        }
        return fcmToken;
    }

    /** Xóa FCM token đã hết hiệu lực của user */
    @Transactional
    public void clearFcmToken(Integer id, String fcmToken) {
        userRepository.clearFcmToken(id, fcmToken);
    }

    /** Tìm user theo id: báo lỗi 400 nếu id không hợp lệ, 404 nếu không tồn tại */
    private User getUser(Integer id) {
        if (id <= 0) {
            throw new BadRequestException("Id phải lớn hơn 0");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với id: " + id));
    }
}
