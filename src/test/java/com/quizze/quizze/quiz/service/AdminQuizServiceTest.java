package com.quizze.quizze.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quizze.quizze.audit.service.AdminAuditLogService;
import com.quizze.quizze.cache.service.QuizCacheInvalidationService;
import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.quiz.domain.Category;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.dto.admin.OptionRequest;
import com.quizze.quizze.quiz.dto.admin.QuestionRequest;
import com.quizze.quizze.quiz.dto.admin.QuestionResponse;
import com.quizze.quizze.quiz.dto.admin.QuizRequest;
import com.quizze.quizze.quiz.dto.admin.QuizResponse;
import com.quizze.quizze.quiz.mapper.AdminQuizMapper;
import com.quizze.quizze.quiz.repository.CategoryRepository;
import com.quizze.quizze.quiz.repository.QuestionRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminQuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AdminAuditLogService adminAuditLogService;

    @Mock
    private QuizCacheInvalidationService quizCacheInvalidationService;

    private AdminQuizService adminQuizService;

    @BeforeEach
    void setUp() {
        adminQuizService = new AdminQuizService(
                quizRepository,
                questionRepository,
                categoryRepository,
                new AdminQuizMapper(),
                adminAuditLogService,
                quizCacheInvalidationService
        );
    }

    @Test
    void createQuizShouldCreateMissingCategoryAndAuditAction() {
        QuizRequest request = new QuizRequest();
        request.setTitle("Java Basics");
        request.setDescription("Intro quiz");
        request.setCategoryName("Programming");
        request.setDifficulty(DifficultyLevel.EASY);
        request.setTimeLimitInMinutes(15);
        request.setPublished(true);

        Category category = new Category();
        category.setId(5L);
        category.setName("Programming");

        when(categoryRepository.findByNameIgnoreCase("Programming")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(11L);
            return quiz;
        });

        QuizResponse response = adminQuizService.createQuiz(1L, "admin", request);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        verify(adminAuditLogService).recordAction(eq(1L), eq("admin"), any(), eq("QUIZ"), eq(11L), eq("Java Basics"), any());
        verify(quizCacheInvalidationService).evictAnalyticsForQuiz(11L);

        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Programming");
        assertThat(response.getCategoryName()).isEqualTo("Programming");
        assertThat(response.getTitle()).isEqualTo("Java Basics");
    }

    @Test
    void addQuestionShouldRejectDuplicateOptions() {
        Quiz quiz = new Quiz();
        quiz.setId(9L);
        quiz.setTitle("Java Basics");

        QuestionRequest request = new QuestionRequest();
        request.setContent("Which keyword inherits a class?");
        request.setPoints(5);
        request.setOptions(List.of(
                option("extends", true),
                option("extends", false)
        ));

        when(quizRepository.findById(9L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> adminQuizService.addQuestion(1L, "admin", 9L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Question options must be unique");

        verify(questionRepository, never()).save(any(Question.class));
    }

    private OptionRequest option(String content, boolean correct) {
        OptionRequest option = new OptionRequest();
        option.setContent(content);
        option.setCorrect(correct);
        return option;
    }
}
