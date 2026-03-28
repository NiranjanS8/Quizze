package com.quizze.quizze.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quizze.quizze.auth.domain.PasswordResetOtp;
import com.quizze.quizze.auth.dto.AuthResponse;
import com.quizze.quizze.auth.dto.ForgotPasswordRequest;
import com.quizze.quizze.auth.dto.RegisterRequest;
import com.quizze.quizze.auth.dto.ResetPasswordRequest;
import com.quizze.quizze.auth.event.UserRegisteredEvent;
import com.quizze.quizze.auth.mapper.AuthMapper;
import com.quizze.quizze.auth.repository.PasswordResetOtpRepository;
import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import com.quizze.quizze.notification.service.PasswordResetEmailService;
import com.quizze.quizze.security.jwt.JwtService;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.RoleRepository;
import com.quizze.quizze.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ApplicationMetricsService applicationMetricsService;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordResetOtpRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                new AuthMapper(),
                applicationEventPublisher,
                applicationMetricsService,
                passwordResetEmailService
        );
    }

    @Test
    void registerShouldPersistUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Niranjan");
        request.setLastName("Kumar");
        request.setEmail("niranjan@example.com");
        request.setUsername("niranjan");
        request.setPassword("Password123");
        request.setNewQuizNotificationsEnabled(true);

        Role userRole = new Role();
        userRole.setName(RoleType.USER);

        when(userRepository.existsByEmail("niranjan@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("niranjan")).thenReturn(false);
        when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(jwtService.generateToken(10L, "niranjan", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("niranjan");
        assertThat(response.getRole()).isEqualTo("USER");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isNewQuizNotificationsEnabled()).isTrue();
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().user().getId()).isEqualTo(10L);
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Niranjan");
        request.setLastName("Kumar");
        request.setEmail("niranjan@example.com");
        request.setUsername("niranjan");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("niranjan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void forgotPasswordShouldCreateOtpAndSendMailForExistingUser() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        User user = new User();
        user.setId(7L);
        user.setEmail("test@example.com");
        user.setFirstName("Test");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");

        String response = authService.forgotPassword(request);

        ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(passwordResetOtpRepository).save(otpCaptor.capture());
        verify(passwordResetEmailService).sendOtp(any(User.class), anyString());

        PasswordResetOtp savedOtp = otpCaptor.getValue();
        assertThat(response).isEqualTo("If an account exists for that email, an OTP has been sent");
        assertThat(savedOtp.getEmail()).isEqualTo("test@example.com");
        assertThat(savedOtp.getOtpHash()).isEqualTo("encoded-otp");
        assertThat(savedOtp.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(9));
    }

    @Test
    void resetPasswordShouldInvalidateOtpAfterMaxFailedAttempts() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("111111");
        request.setNewPassword("NewPassword123");

        User user = new User();
        user.setId(21L);

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail("test@example.com");
        otp.setUser(user);
        otp.setOtpHash("stored-hash");
        otp.setFailedAttempts(4);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc("test@example.com")).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("111111", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("OTP attempt limit exceeded. Request a new code.");

        assertThat(otp.getFailedAttempts()).isEqualTo(5);
        assertThat(otp.getUsedAt()).isNotNull();
    }
}
