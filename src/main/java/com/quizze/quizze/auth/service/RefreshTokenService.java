package com.quizze.quizze.auth.service;

import com.quizze.quizze.auth.config.RefreshTokenProperties;
import com.quizze.quizze.auth.domain.RefreshToken;
import com.quizze.quizze.auth.repository.RefreshTokenRepository;
import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String create(User user) {
        String token = generateToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(token));
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(properties.getExpirationMs())));
        refreshTokenRepository.save(refreshToken);

        return token;
    }

    @Transactional
    public RefreshToken consume(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid refresh token");
        }

        refreshToken.setRevokedAt(LocalDateTime.now());
        return refreshToken;
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(hash(token))
                .ifPresent(refreshToken -> refreshToken.setRevokedAt(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(refreshToken -> refreshToken.setRevokedAt(now));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
