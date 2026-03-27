package com.quizze.quizze.admin.controller;

import com.quizze.quizze.common.api.ApiResponse;
import com.quizze.quizze.quiz.dto.admin.QuestionRequest;
import com.quizze.quizze.quiz.dto.admin.QuestionResponse;
import com.quizze.quizze.quiz.dto.admin.QuizRequest;
import com.quizze.quizze.quiz.dto.admin.QuizResponse;
import com.quizze.quizze.quiz.service.AdminQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Quiz Management", description = "Admin-only endpoints for managing quizzes and questions")
public class AdminController {

    private final AdminQuizService adminQuizService;

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
            @Valid @RequestBody QuizRequest request
    ) {
        QuizResponse response = adminQuizService.createQuiz(request);
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
            @PathVariable Long id,
            @Valid @RequestBody QuizRequest request
    ) {
        QuizResponse response = adminQuizService.updateQuiz(id, request);
        return ResponseEntity.ok(ApiResponse.success("Quiz updated successfully", response));
    }

    @DeleteMapping("/quizzes/{id}")
    @Operation(
            summary = "Delete quiz",
            description = "Deletes a quiz and its associated questions.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Long id) {
        adminQuizService.deleteQuiz(id);
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
            @PathVariable Long id,
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
            @Valid @RequestBody QuestionRequest request
    ) {
        QuestionResponse response = adminQuizService.addQuestion(id, request);
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
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request
    ) {
        QuestionResponse response = adminQuizService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Question updated successfully", response));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(
            summary = "Delete question",
            description = "Deletes a question from its quiz.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        adminQuizService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully"));
    }
}
