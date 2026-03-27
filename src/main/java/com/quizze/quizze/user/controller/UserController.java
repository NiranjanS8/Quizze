package com.quizze.quizze.user.controller;

import com.quizze.quizze.common.api.ApiResponse;
import com.quizze.quizze.security.user.CustomUserDetails;
import com.quizze.quizze.user.dto.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
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
