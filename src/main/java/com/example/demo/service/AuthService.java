package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatusEnum;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Đăng nhập và cấp token, báo lỗi 401 nếu sai email, sai mật khẩu hoặc tài khoản bị vô hiệu hóa */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng"));
        return issueTokens(requireActive(user));
    }

    /** Cấp cặp token mới từ refresh token, báo lỗi 401 nếu token không hợp lệ hoặc user không còn hoạt động */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        return issueTokens(getActiveUser(jwtService.parseRefreshToken(request.refreshToken())));
    }

    /** Lấy user đang đăng nhập từ header Authorization, báo lỗi 401 nếu token không hợp lệ hoặc user không còn hoạt động */
    @Transactional(readOnly = true)
    public UserResponse me(String authorization) {
        return UserResponse.from(getActiveUser(jwtService.parseAccessToken(extractBearerToken(authorization))));
    }

    /** Xác thực header Authorization, báo lỗi 401 nếu token không hợp lệ, user đã bị xóa, hoặc user INACTIVE khi allowInactive là false */
    @Transactional(readOnly = true)
    public void authenticate(String authorization, boolean allowInactive) {
        User user = getUser(jwtService.parseAccessToken(extractBearerToken(authorization)));
        if (!allowInactive) {
            requireActive(user);
        }
    }

    private TokenResponse issueTokens(User user) {
        return new TokenResponse(
                jwtService.createAccessToken(user.getId()),
                jwtService.createRefreshToken(user.getId()));
    }

    private User getActiveUser(Integer id) {
        return requireActive(getUser(id));
    }

    private User getUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Token không hợp lệ hoặc đã hết hạn"));
    }

    private User requireActive(User user) {
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
        }
        return user;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedException("Thiếu access token");
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
