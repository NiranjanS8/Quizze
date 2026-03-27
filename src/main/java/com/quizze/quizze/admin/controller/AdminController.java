package com.quizze.quizze.admin.controller;

import com.quizze.quizze.common.api.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/access-check")
    public ResponseEntity<ApiResponse<Map<String, String>>> accessCheck() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin access granted",
                Map.of("status", "You are authorized as ADMIN")
        ));
    }
}
