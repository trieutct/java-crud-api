package com.example.demo.service;

import com.example.demo.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /** Tạo access token cho user */
    public String createAccessToken(Integer userId) {
        return createToken(userId, ACCESS_TYPE, accessTokenTtl);
    }

    /** Tạo refresh token cho user */
    public String createRefreshToken(Integer userId) {
        return createToken(userId, REFRESH_TYPE, refreshTokenTtl);
    }

    /** Lấy id user từ access token, báo lỗi 401 nếu token không hợp lệ hoặc hết hạn */
    public Integer parseAccessToken(String token) {
        return parseToken(token, ACCESS_TYPE);
    }

    /** Lấy id user từ refresh token, báo lỗi 401 nếu token không hợp lệ hoặc hết hạn */
    public Integer parseRefreshToken(String token) {
        return parseToken(token, REFRESH_TYPE);
    }

    private String createToken(Integer userId, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Kiểm tra chữ ký, hạn dùng và loại token để refresh token không dùng được thay access token */
    private Integer parseToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                return Integer.valueOf(claims.getSubject());
            }
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Token không hợp lệ hoặc đã hết hạn");
        }
        throw new UnauthorizedException("Token không hợp lệ hoặc đã hết hạn");
    }
}
