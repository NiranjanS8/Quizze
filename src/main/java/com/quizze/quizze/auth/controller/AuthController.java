package com.quizze.quizze.auth.controller;

import com.quizze.quizze.auth.dto.AuthResponse;
import com.quizze.quizze.auth.dto.ForgotPasswordRequest;
import com.quizze.quizze.auth.dto.LoginRequest;
import com.quizze.quizze.auth.dto.RegisterRequest;
import com.quizze.quizze.auth.dto.RefreshTokenRequest;
import com.quizze.quizze.auth.dto.ResetPasswordRequest;
import com.quizze.quizze.auth.service.AuthRateLimitService;
import com.quizze.quizze.auth.service.AuthService;
import com.quizze.quizze.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs for user registration and login")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimitService authRateLimitService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new USER account and returns a JWT access token for immediate authenticated access."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content)
    })
    public ResponseEntity<com.quizze.quizze.common.api.ApiResponse<AuthResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Registration payload",
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "Register request",
                                    value = """
                                            {
                                              "firstName": "Niranjan",
                                              "lastName": "Kumar",
                                              "email": "niranjan@example.com",
                                              "username": "niranjan",
                                              "password": "Password123"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.quizze.quizze.common.api.ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticates a user by username or email and returns a JWT access token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<com.quizze.quizze.common.api.ApiResponse<AuthResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Login payload",
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Login request",
                                    value = """
                                            {
                                              "usernameOrEmail": "niranjan",
                                              "password": "Password123"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authRateLimitService.checkLoginLimit(resolveClientIp(httpServletRequest), request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(com.quizze.quizze.common.api.ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Rotates a valid refresh token and returns a new access token and refresh token."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Revokes the provided refresh token."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        String message = authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Send password reset OTP",
            description = "Generates a short-lived OTP and emails it to the user if the account exists."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP flow handled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Forgot password payload",
                    content = @Content(
                            schema = @Schema(implementation = ForgotPasswordRequest.class),
                            examples = @ExampleObject(
                                    name = "Forgot password request",
                                    value = """
                                            {
                                              "email": "niranjan@example.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authRateLimitService.checkForgotPasswordLimit(resolveClientIp(httpServletRequest), request.getEmail());
        String message = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using OTP",
            description = "Verifies the email and OTP combination, then updates the user's password."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or OTP", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Reset password payload",
                    content = @Content(
                            schema = @Schema(implementation = ResetPasswordRequest.class),
                            examples = @ExampleObject(
                                    name = "Reset password request",
                                    value = """
                                            {
                                              "email": "niranjan@example.com",
                                              "otp": "123456",
                                              "newPassword": "Password123"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authRateLimitService.checkResetPasswordLimit(resolveClientIp(httpServletRequest), request.getEmail());
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank() && isTrustedProxy(request.getRemoteAddr())) {
            return forwardedFor.split(",")[0].trim().toLowerCase(Locale.ROOT);
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
