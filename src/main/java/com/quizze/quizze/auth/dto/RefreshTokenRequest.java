package com.quizze.quizze.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(max = 500, message = "Refresh token must not exceed 500 characters")
    private String refreshToken;
}
