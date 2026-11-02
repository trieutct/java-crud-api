package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatusEnum;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final CreateUserRequest CREATE_REQUEST =
            new CreateUserRequest("Nguyễn An", "an@example.com", "matkhau123", "0901234567");
    private static final UpdateUserRequest UPDATE_REQUEST =
            new UpdateUserRequest("Nguyễn An", "an@example.com", "0901234567", UserStatusEnum.INACTIVE);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createHashesPasswordAndDefaultsToActive() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
        when(passwordEncoder.encode("matkhau123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = userService.create(CREATE_REQUEST);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(response.fullName()).isEqualTo("Nguyễn An");
        assertThat(response.phoneNumber()).isEqualTo("0901234567");
        assertThat(response.status()).isEqualTo(UserStatusEnum.ACTIVE);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(CREATE_REQUEST))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsPasswordOver72Bytes() {
        CreateUserRequest request = new CreateUserRequest("An", "an@example.com", "ệ".repeat(25), null);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void rejectsNonPositiveId() {
        assertThatThrownBy(() -> userService.findById(0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.update(-1, UPDATE_REQUEST))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.delete(0))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateChangesFieldsButKeepsPassword() {
        User existing = new User();
        existing.setFullName("Old");
        existing.setEmail("old@example.com");
        existing.setPasswordHash("old-hash");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("an@example.com", 1)).thenReturn(false);
        when(userRepository.saveAndFlush(existing)).thenReturn(existing);

        UserResponse response = userService.update(1, UPDATE_REQUEST);

        assertThat(response.fullName()).isEqualTo("Nguyễn An");
        assertThat(response.email()).isEqualTo("an@example.com");
        assertThat(response.status()).isEqualTo(UserStatusEnum.INACTIVE);
        assertThat(existing.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void updateRejectsEmailOfAnotherUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(userRepository.existsByEmailAndIdNot("an@example.com", 1)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1, UPDATE_REQUEST))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void getFcmTokenReturnsStoredToken() {
        User existing = new User();
        existing.setFcmToken("token-1");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));

        assertThat(userService.getFcmToken(1)).isEqualTo("token-1");
    }

    @Test
    void getFcmTokenRejectsUserWithoutToken() {
        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.getFcmToken(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User chưa đăng ký FCM token");
    }

    @Test
    void getFcmTokenThrowsWhenUserMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getFcmToken(1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void clearFcmTokenOnlyClearsMatchingToken() {
        userService.clearFcmToken(1, "token-1");

        verify(userRepository).clearFcmToken(1, "token-1");
    }

    @Test
    void updateDoesNotTouchFcmToken() {
        User existing = new User();
        existing.setFcmToken("token-1");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("an@example.com", 1)).thenReturn(false);
        when(userRepository.saveAndFlush(existing)).thenReturn(existing);

        userService.update(1, UPDATE_REQUEST);

        assertThat(existing.getFcmToken()).isEqualTo("token-1");
    }

    @Test
    void deleteRemovesExistingUser() {
        User existing = new User();
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));

        userService.delete(1);

        verify(userRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
