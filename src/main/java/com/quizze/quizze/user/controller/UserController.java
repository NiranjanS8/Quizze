package com.quizze.quizze.user.controller;

import com.quizze.quizze.common.api.ApiResponse;
import com.quizze.quizze.security.user.CustomUserDetails;
import com.quizze.quizze.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints for authenticated user operations")
public class UserController {

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's profile information.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(currentUser.getUser().getId())
                .firstName(currentUser.getUser().getFirstName())
                .lastName(currentUser.getUser().getLastName())
                .username(currentUser.getUser().getUsername())
                .email(currentUser.getUser().getEmail())
                .role(currentUser.getUser().getRole().getName().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", response));
    }
}
