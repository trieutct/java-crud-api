package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.dto.UpdateFcmTokenRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatusEnum;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final LoginRequest LOGIN_REQUEST = new LoginRequest("an@example.com", "matkhau123");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user(UserStatusEnum status) {
        User user = new User();
        user.setEmail("an@example.com");
        user.setPasswordHash("hashed");
        user.setStatus(status);
        return user;
    }

    @Test
    void loginReturnsTokens() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user(UserStatusEnum.ACTIVE)));
        when(passwordEncoder.matches("matkhau123", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(null)).thenReturn("access");
        when(jwtService.createRefreshToken(null)).thenReturn("refresh");

        TokenResponse response = authService.login(LOGIN_REQUEST);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(LOGIN_REQUEST))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email hoặc mật khẩu không đúng");
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user(UserStatusEnum.ACTIVE)));
        when(passwordEncoder.matches("matkhau123", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(LOGIN_REQUEST))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email hoặc mật khẩu không đúng");
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRejectsInactiveUser() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user(UserStatusEnum.INACTIVE)));
        when(passwordEncoder.matches("matkhau123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(LOGIN_REQUEST))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Tài khoản đã bị vô hiệu hóa");
        verifyNoInteractions(jwtService);
    }

    @Test
    void refreshReturnsNewTokens() {
        when(jwtService.parseRefreshToken("cu")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(UserStatusEnum.ACTIVE)));
        when(jwtService.createAccessToken(null)).thenReturn("access");
        when(jwtService.createRefreshToken(null)).thenReturn("refresh");

        TokenResponse response = authService.refresh(new RefreshTokenRequest("cu"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void refreshRejectsDeletedUser() {
        when(jwtService.parseRefreshToken("cu")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("cu")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void meReturnsCurrentUser() {
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(UserStatusEnum.ACTIVE)));

        UserResponse response = authService.me("Bearer abc");

        assertThat(response.email()).isEqualTo("an@example.com");
    }

    @Test
    void meRejectsInactiveUser() {
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(UserStatusEnum.INACTIVE)));

        assertThatThrownBy(() -> authService.me("Bearer abc"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateFcmTokenSavesTokenForCurrentUser() {
        User current = user(UserStatusEnum.ACTIVE);
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(current));
        when(userRepository.saveAndFlush(current)).thenReturn(current);

        UserResponse response = authService.updateFcmToken("Bearer abc", new UpdateFcmTokenRequest("token-1"));

        assertThat(current.getFcmToken()).isEqualTo("token-1");
        assertThat(response.email()).isEqualTo("an@example.com");
    }

    @Test
    void updateFcmTokenRejectsInactiveUser() {
        User current = user(UserStatusEnum.INACTIVE);
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> authService.updateFcmToken("Bearer abc", new UpdateFcmTokenRequest("token-1")))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(current.getFcmToken()).isNull();
    }

    @Test
    void authenticateRejectsInactiveUserByDefault() {
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(UserStatusEnum.INACTIVE)));

        assertThatThrownBy(() -> authService.authenticate("Bearer abc", false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Tài khoản đã bị vô hiệu hóa");
    }

    @Test
    void authenticateAcceptsInactiveUserWhenAllowed() {
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(UserStatusEnum.INACTIVE)));

        authService.authenticate("Bearer abc", true);

        verify(userRepository).findById(1);
    }

    @Test
    void authenticateRejectsDeletedUserEvenWhenInactiveAllowed() {
        when(jwtService.parseAccessToken("abc")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("Bearer abc", true))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void authenticateRejectsMissingTokenEvenWhenInactiveAllowed() {
        assertThatThrownBy(() -> authService.authenticate(null, true))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void meRejectsMissingOrNonBearerHeader() {
        assertThatThrownBy(() -> authService.me(null))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authService.me("Basic abc"))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(jwtService, userRepository);
    }
}
