package com.quizze.quizze.quiz.controller;

import com.quizze.quizze.common.api.ApiResponse;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardResponse;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.dto.user.AttemptQuestionsResponse;
import com.quizze.quizze.quiz.dto.user.QuizCatalogResponse;
import com.quizze.quizze.quiz.dto.user.QuizDetailResponse;
import com.quizze.quizze.quiz.dto.user.QuizResultResponse;
import com.quizze.quizze.quiz.dto.user.StartQuizResponse;
import com.quizze.quizze.quiz.dto.user.SubmitQuizRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizResponse;
import com.quizze.quizze.quiz.service.UserQuizService;
import com.quizze.quizze.quiz.service.QuizLeaderboardService;
import com.quizze.quizze.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "User Quiz Flow", description = "User-facing endpoints for browsing quizzes and managing quiz attempts")
public class UserQuizController {

    private final UserQuizService userQuizService;
    private final QuizLeaderboardService quizLeaderboardService;

    @GetMapping
    @Operation(
            summary = "List published quizzes",
            description = "Returns published quizzes with search, filter, sorting, and pagination support. Correct answers are never exposed.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizCatalogResponse>> getPublishedQuizzes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Published quizzes fetched successfully",
                userQuizService.getPublishedQuizzes(search, category, difficulty, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get published quiz details",
            description = "Returns published quiz metadata without exposing answer keys.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getPublishedQuizDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Quiz details fetched successfully",
                userQuizService.getPublishedQuizDetails(id)
        ));
    }

    @GetMapping("/{id}/leaderboard")
    @Operation(
            summary = "Get published quiz leaderboard",
            description = "Returns the top submitted scores for a published quiz, ranked by score and submission time.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<QuizLeaderboardResponse>> getPublishedQuizLeaderboard(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Quiz leaderboard fetched successfully",
                quizLeaderboardService.getLeaderboard(id, limit, true)
        ));
    }

    @PostMapping("/{id}/start")
    @Operation(
            summary = "Start quiz attempt",
            description = "Creates a new attempt for the authenticated user on a published quiz.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<StartQuizResponse>> startQuiz(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        StartQuizResponse response = userQuizService.startQuiz(id, currentUser.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz attempt started successfully", response));
    }

    @GetMapping("/attempts/{attemptId}/questions")
    @Operation(
            summary = "Fetch attempt questions",
            description = "Returns the questions and options for a user's specific attempt without correct-answer metadata.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AttemptQuestionsResponse>> getAttemptQuestions(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Attempt questions fetched successfully",
                userQuizService.getAttemptQuestions(attemptId, currentUser.getUser().getId())
        ));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(
            summary = "Submit quiz attempt",
            description = "Stores submitted answers, evaluates them, and marks the attempt as submitted.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Quiz submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid submission payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attempt not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SubmitQuizResponse>> submitQuiz(
            @PathVariable Long attemptId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Answer submission payload",
                    content = @Content(
                            schema = @Schema(implementation = SubmitQuizRequest.class),
                            examples = @ExampleObject(
                                    name = "Submit quiz request",
                                    value = """
                                            {
                                              "answers": [
                                                { "questionId": 1, "selectedOptionId": 2 },
                                                { "questionId": 2, "selectedOptionId": 8 }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SubmitQuizRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        SubmitQuizResponse response = userQuizService.submitQuiz(attemptId, currentUser.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted successfully", response));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(
            summary = "Get attempt result summary",
            description = "Returns the evaluated result summary for a submitted attempt, including score, percentage, and per-question outcomes.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Result fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Result not yet available", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attempt not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<QuizResultResponse>> getAttemptResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Result fetched successfully",
                userQuizService.getAttemptResult(attemptId, currentUser.getUser().getId())
        ));
    }
}
