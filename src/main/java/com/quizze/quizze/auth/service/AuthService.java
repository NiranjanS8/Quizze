package com.quizze.quizze.auth.service;

import com.quizze.quizze.auth.dto.AuthResponse;
import com.quizze.quizze.auth.dto.ForgotPasswordRequest;
import com.quizze.quizze.auth.dto.LoginRequest;
import com.quizze.quizze.auth.dto.RegisterRequest;
import com.quizze.quizze.auth.dto.ResetPasswordRequest;
import com.quizze.quizze.auth.domain.PasswordResetOtp;
import com.quizze.quizze.auth.mapper.AuthMapper;
import com.quizze.quizze.auth.repository.PasswordResetOtpRepository;
import com.quizze.quizze.notification.service.WelcomeEmailService;
import com.quizze.quizze.notification.service.PasswordResetEmailService;
import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.security.jwt.JwtService;
import com.quizze.quizze.security.user.CustomUserDetails;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.RoleRepository;
import com.quizze.quizze.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final String FORGOT_PASSWORD_RESPONSE_MESSAGE =
            "If an account exists for that email, an OTP has been sent";
    private static final int PASSWORD_RESET_OTP_EXPIRY_MINUTES = 10;
    private static final int PASSWORD_RESET_RESEND_COOLDOWN_SECONDS = 60;
    private static final int PASSWORD_RESET_MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final WelcomeEmailService welcomeEmailService;
    private final PasswordResetEmailService passwordResetEmailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim();
        log.info("Registering new user with username='{}' and email='{}'", normalizedUsername, normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration blocked because email already exists: {}", normalizedEmail);
            throw new EntityExistsException("Email is already in use");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            log.warn("Registration blocked because username already exists: {}", normalizedUsername);
            throw new EntityExistsException("Username is already in use");
        }

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new UsernameNotFoundException("Default user role is not configured"));

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole().getName().name());
        log.info("User registered successfully with userId={} and role={}", savedUser.getId(), savedUser.getRole().getName());
        welcomeEmailService.sendWelcomeEmail(savedUser);

        return authMapper.toAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        String principalInput = request.getUsernameOrEmail().trim();
        log.info("Authenticating user with principal='{}'", principalInput);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(principalInput, request.getPassword())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().getName().name());
        log.info("User authenticated successfully with userId={} and role={}", user.getId(), user.getRole().getName());

        return authMapper.toAuthResponse(user, token);
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        log.info("Processing forgot password request for email='{}'", normalizedEmail);

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(user -> {
            enforceResendCooldown(normalizedEmail);
            invalidateActiveOtps(normalizedEmail);

            String otp = generateOtp();
            PasswordResetOtp passwordResetOtp = new PasswordResetOtp();
            passwordResetOtp.setUser(user);
            passwordResetOtp.setEmail(normalizedEmail);
            passwordResetOtp.setOtpHash(passwordEncoder.encode(otp));
            passwordResetOtp.setExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_OTP_EXPIRY_MINUTES));
            passwordResetOtpRepository.save(passwordResetOtp);
            passwordResetEmailService.sendOtp(user, otp);

            log.info("Password reset OTP generated for userId={}", user.getId());
        }, () -> log.info("Forgot password requested for non-existent email='{}'", normalizedEmail));

        return FORGOT_PASSWORD_RESPONSE_MESSAGE;
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        log.info("Resetting password using OTP for email='{}'", normalizedEmail);

        PasswordResetOtp latestOtp = passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (latestOtp.getUsedAt() != null || latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        if (!passwordEncoder.matches(request.getOtp(), latestOtp.getOtpHash())) {
            int nextFailedAttempts = latestOtp.getFailedAttempts() + 1;
            latestOtp.setFailedAttempts(nextFailedAttempts);
            if (nextFailedAttempts >= PASSWORD_RESET_MAX_OTP_ATTEMPTS) {
                latestOtp.setUsedAt(LocalDateTime.now());
                throw new BadRequestException("OTP attempt limit exceeded. Request a new code.");
            }
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = latestOtp.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        latestOtp.setUsedAt(LocalDateTime.now());
        invalidateActiveOtps(normalizedEmail);

        log.info("Password reset completed successfully for userId={}", user.getId());
        return "Password reset successfully. You can now sign in.";
    }

    private void enforceResendCooldown(String email) {
        passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(otp -> otp.getUsedAt() == null)
                .filter(otp -> otp.getCreatedAt() != null)
                .ifPresent(otp -> {
                    long secondsSinceLastOtp = Duration.between(otp.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    if (secondsSinceLastOtp < PASSWORD_RESET_RESEND_COOLDOWN_SECONDS) {
                        long secondsRemaining = PASSWORD_RESET_RESEND_COOLDOWN_SECONDS - secondsSinceLastOtp;
                        throw new BadRequestException("Please wait " + secondsRemaining + " seconds before requesting another OTP");
                    }
                });
    }

    private void invalidateActiveOtps(String email) {
        LocalDateTime now = LocalDateTime.now();
        passwordResetOtpRepository.findByEmailAndUsedAtIsNull(email)
                .forEach(otp -> otp.setUsedAt(now));
    }

    private String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int value = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}
