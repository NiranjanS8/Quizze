package com.quizze.quizze.admin.controller;

import com.quizze.quizze.audit.dto.AdminAuditLogResponse;
import com.quizze.quizze.audit.service.AdminAuditLogService;
import com.quizze.quizze.common.api.ApiResponse;
import com.quizze.quizze.quiz.dto.analytics.QuizPerformanceAnalyticsResponse;
import com.quizze.quizze.quiz.dto.analytics.AdminOverviewResponse;
import com.quizze.quizze.quiz.dto.analytics.QuestionAnalyticsResponse;
import com.quizze.quizze.quiz.dto.admin.QuestionRequest;
import com.quizze.quizze.quiz.dto.admin.QuestionResponse;
import com.quizze.quizze.quiz.dto.admin.QuizRequest;
import com.quizze.quizze.quiz.dto.admin.QuizResponse;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardResponse;
import com.quizze.quizze.quiz.service.AdminQuizService;
import com.quizze.quizze.quiz.service.AdminOverviewService;
import com.quizze.quizze.quiz.service.QuizAnalyticsService;
import com.quizze.quizze.quiz.service.QuizLeaderboardService;
import com.quizze.quizze.quiz.service.QuestionAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import com.quizze.quizze.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Quiz Management", description = "Admin-only endpoints for managing quizzes and questions")
public class AdminController {

    private final AdminQuizService adminQuizService;
    private final AdminOverviewService adminOverviewService;
    private final QuizAnalyticsService quizAnalyticsService;
    private final QuizLeaderboardService quizLeaderboardService;
    private final QuestionAnalyticsService questionAnalyticsService;
    private final AdminAuditLogService adminAuditLogService;

    @GetMapping("/access-check")
    @Operation(
            summary = "Verify admin access",
            description = "Simple endpoint to confirm that the authenticated user has ADMIN access.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> accessCheck() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin access granted",
                Map.of("status", "You are authorized as ADMIN")
        ));
    }

    @GetMapping("/analytics/overview")
    @Operation(
            summary = "Get admin overview analytics",
            description = "Returns platform-wide admin dashboard metrics including users, quizzes, attempts, most attempted quizzes, and top performing quizzes.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverviewAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin overview analytics fetched successfully",
                adminOverviewService.getOverview()
        ));
    }

    @GetMapping("/quizzes")
    @Operation(
            summary = "List all quizzes",
            description = "Returns all quizzes, including drafts and published items, for admin management.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<java.util.List<QuizResponse>>> getAllQuizzes() {
        return ResponseEntity.ok(ApiResponse.success(
                "Quizzes fetched successfully",
                adminQuizService.getAllQuizzes()
        ));
    }

    @GetMapping("/quizzes/{id}")
    @Operation(
            summary = "Get quiz details",
            description = "Returns full quiz metadata and questions for an admin-managed quiz.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Quiz fetched successfully",
                adminQuizService.getQuiz(id)
        ));
    }

    @GetMapping("/quizzes/{id}/leaderboard")
    @Operation(
            summary = "Get quiz leaderboard",
            description = "Returns ranked submitted attempts for a quiz, ordered by highest score and earliest submission time.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizLeaderboardResponse>> getQuizLeaderboard(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Quiz leaderboard fetched successfully",
                quizLeaderboardService.getLeaderboard(id, limit, false)
        ));
    }

    @GetMapping("/quizzes/{id}/analytics")
    @Operation(
            summary = "Get quiz performance analytics",
            description = "Returns quiz-level attempt and scoring analytics for admin reporting.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizPerformanceAnalyticsResponse>> getQuizAnalytics(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Quiz analytics fetched successfully",
                quizAnalyticsService.getQuizPerformanceAnalytics(id)
        ));
    }

    @GetMapping("/quizzes/{id}/questions/analytics")
    @Operation(
            summary = "Get question-level analytics",
            description = "Returns hardest and easiest questions for a quiz based on submitted answer accuracy.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuestionAnalyticsResponse>> getQuestionAnalytics(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Question analytics fetched successfully",
                questionAnalyticsService.getQuestionAnalytics(id)
        ));
    }

    @GetMapping("/audit-logs")
    @Operation(
            summary = "Get recent admin audit logs",
            description = "Returns recent admin actions for quiz and question management.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<java.util.List<AdminAuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin audit logs fetched successfully",
                adminAuditLogService.getRecentLogs(limit)
        ));
    }

    @PostMapping("/quizzes")
    @Operation(
            summary = "Create quiz",
            description = "Creates a new quiz with metadata such as category, difficulty, publication status, and attempt rules.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Quiz created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
    })
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Quiz creation payload",
                    content = @Content(
                            schema = @Schema(implementation = QuizRequest.class),
                            examples = @ExampleObject(
                                    name = "Create quiz request",
                                    value = """
                                            {
                                              "title": "Java Basics",
                                              "description": "Fundamentals of Java programming",
                                              "categoryName": "Programming",
                                              "difficulty": "EASY",
                                              "timeLimitInMinutes": 15,
                                              "published": false,
                                              "negativeMarkingEnabled": false,
                                              "oneAttemptOnly": false
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody QuizRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QuizResponse response = adminQuizService.createQuiz(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz created successfully", response));
    }

    @PutMapping("/quizzes/{id}")
    @Operation(
            summary = "Update quiz",
            description = "Updates an existing quiz's metadata and availability settings.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable @Positive Long id,
            @Valid @RequestBody QuizRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QuizResponse response = adminQuizService.updateQuiz(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("Quiz updated successfully", response));
    }

    @DeleteMapping("/quizzes/{id}")
    @Operation(
            summary = "Delete quiz",
            description = "Deletes a quiz and its associated questions.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        adminQuizService.deleteQuiz(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                id
        );
        return ResponseEntity.ok(ApiResponse.success("Quiz deleted successfully"));
    }

    @PostMapping("/quizzes/{id}/questions")
    @Operation(
            summary = "Add question to quiz",
            description = "Adds a multiple-choice question to an existing quiz. Exactly one option must be marked as correct.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Question added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or question options are invalid", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable @Positive Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Question creation payload",
                    content = @Content(
                            schema = @Schema(implementation = QuestionRequest.class),
                            examples = @ExampleObject(
                                    name = "Add question request",
                                    value = """
                                            {
                                              "content": "Which keyword is used to inherit a class in Java?",
                                              "points": 5,
                                              "options": [
                                                { "content": "implements", "correct": false },
                                                { "content": "extends", "correct": true },
                                                { "content": "inherits", "correct": false },
                                                { "content": "instanceof", "correct": false }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QuestionResponse response = adminQuizService.addQuestion(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                id,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question added successfully", response));
    }

    @PutMapping("/questions/{id}")
    @Operation(
            summary = "Update question",
            description = "Updates question text, points, and options for an existing question.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable @Positive Long id,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QuestionResponse response = adminQuizService.updateQuestion(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("Question updated successfully", response));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(
            summary = "Delete question",
            description = "Deletes a question from its quiz.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        adminQuizService.deleteQuestion(
                currentUser.getUser().getId(),
                currentUser.getUser().getUsername(),
                id
        );
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully"));
    }
}
