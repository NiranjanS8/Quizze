package com.quizze.quizze.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String email;
    private final String role;
}
