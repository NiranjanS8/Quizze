package com.quizze.quizze.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final Long userId;
    private final String username;
    private final String email;
    private final String role;
}
