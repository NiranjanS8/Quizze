package com.quizze.quizze.auth.service;

import com.quizze.quizze.auth.dto.AuthResponse;
import com.quizze.quizze.auth.dto.LoginRequest;
import com.quizze.quizze.auth.dto.RegisterRequest;
import com.quizze.quizze.security.jwt.JwtService;
import com.quizze.quizze.security.user.CustomUserDetails;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.RoleRepository;
import com.quizze.quizze.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EntityExistsException("Email is already in use");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
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

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().getName().name());

        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .build();
    }
}
