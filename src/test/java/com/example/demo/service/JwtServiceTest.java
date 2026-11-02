package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.exception.UnauthorizedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "khoa-chi-dung-cho-test-dai-tren-32-byte";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(15), Duration.ofDays(7));

    @Test
    void parsesUserIdFromOwnTokens() {
        assertThat(jwtService.parseAccessToken(jwtService.createAccessToken(1))).isEqualTo(1);
        assertThat(jwtService.parseRefreshToken(jwtService.createRefreshToken(1))).isEqualTo(1);
    }

    @Test
    void rejectsTokenOfWrongType() {
        String accessToken = jwtService.createAccessToken(1);
        String refreshToken = jwtService.createRefreshToken(1);

        assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expired = new JwtService(SECRET, Duration.ofMinutes(-1), Duration.ofMinutes(-1));

        assertThatThrownBy(() -> jwtService.parseAccessToken(expired.createAccessToken(1)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsTokenSignedWithOtherKey() {
        JwtService other = new JwtService(SECRET + "-khac", Duration.ofMinutes(15), Duration.ofDays(7));

        assertThatThrownBy(() -> jwtService.parseAccessToken(other.createAccessToken(1)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsMalformedAndBlankToken() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("khong-phai-jwt"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwtService.parseAccessToken(""))
                .isInstanceOf(UnauthorizedException.class);
    }
}
