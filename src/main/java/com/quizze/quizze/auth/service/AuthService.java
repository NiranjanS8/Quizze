package com.quizze.quizze.auth.service;

import com.quizze.quizze.auth.dto.AuthResponse;
import com.quizze.quizze.auth.dto.LoginRequest;
import com.quizze.quizze.auth.dto.RegisterRequest;
import com.quizze.quizze.auth.mapper.AuthMapper;
import com.quizze.quizze.notification.service.WelcomeEmailService;
import com.quizze.quizze.security.jwt.JwtService;
import com.quizze.quizze.security.user.CustomUserDetails;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.RoleRepository;
import com.quizze.quizze.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final WelcomeEmailService welcomeEmailService;

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
}
