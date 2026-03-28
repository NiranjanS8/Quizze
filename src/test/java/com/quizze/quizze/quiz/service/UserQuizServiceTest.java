package com.quizze.quizze.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import com.quizze.quizze.quiz.dto.user.SubmitAnswerRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizResponse;
import com.quizze.quizze.quiz.mapper.UserQuizMapper;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserQuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ApplicationMetricsService applicationMetricsService;

    private UserQuizService userQuizService;

    @BeforeEach
    void setUp() {
        userQuizService = new UserQuizService(
                quizRepository,
                quizAttemptRepository,
                userRepository,
                new UserQuizMapper(),
                applicationEventPublisher,
                applicationMetricsService
        );
    }

    @Test
    void startQuizShouldRejectSecondAttemptForSingleAttemptQuiz() {
        Quiz quiz = new Quiz();
        quiz.setId(12L);
        quiz.setTitle("Spring Boot");
        quiz.setPublished(true);
        quiz.setOneAttemptOnly(true);
        quiz.getQuestions().add(question(1L, "Question", 5, option(11L, "Answer", true)));

        User user = user(3L, "learner");

        when(quizRepository.findByIdAndPublishedTrue(12L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(quizAttemptRepository.findByUserIdAndQuizId(3L, 12L)).thenReturn(List.of(new QuizAttempt()));

        assertThatThrownBy(() -> userQuizService.startQuiz(12L, 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This quiz can only be attempted once");
    }

    @Test
    void submitQuizShouldApplyNegativeMarkingAndPublishSubmissionEvent() {
        Quiz quiz = new Quiz();
        quiz.setId(25L);
        quiz.setTitle("Java Fundamentals");
        quiz.setDifficulty(DifficultyLevel.MEDIUM);
        quiz.setNegativeMarkingEnabled(true);

        Question q1 = question(
                101L,
                "Inheritance keyword",
                5,
                option(1001L, "extends", true),
                option(1002L, "implements", false)
        );
        Question q2 = question(
                102L,
                "Parent reference type",
                4,
                option(1003L, "upcasting", true),
                option(1004L, "this", false)
        );
        q1.setQuiz(quiz);
        q2.setQuiz(quiz);
        quiz.getQuestions().addAll(List.of(q1, q2));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(50L);
        attempt.setQuiz(quiz);
        attempt.setUser(user(7L, "learner"));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        attempt.setQuestionOrder("101,102");

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(
                answer(101L, 1001L),
                answer(102L, 1004L)
        ));

        when(quizAttemptRepository.findByIdAndUserId(50L, 7L)).thenReturn(Optional.of(attempt));

        SubmitQuizResponse response = userQuizService.submitQuiz(50L, 7L, request);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getCorrectAnswers()).isEqualTo(1);
        assertThat(attempt.getWrongAnswers()).isEqualTo(1);
        assertThat(attempt.getScore()).isEqualTo(4.0);
        assertThat(response.getPercentage()).isEqualTo(44.44444444444444);
        ArgumentCaptor<QuizSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(QuizSubmittedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().quizId()).isEqualTo(25L);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().attemptId()).isEqualTo(50L);
    }

    private User user(Long id, String username) {
        Role role = new Role();
        role.setName(RoleType.USER);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private Question question(Long id, String content, int points, Option... options) {
        Question question = new Question();
        question.setId(id);
        question.setContent(content);
        question.setPoints(points);
        for (Option option : options) {
            option.setQuestion(question);
            question.getOptions().add(option);
        }
        return question;
    }

    private Option option(Long id, String content, boolean correct) {
        Option option = new Option();
        option.setId(id);
        option.setContent(content);
        option.setCorrect(correct);
        return option;
    }

    private SubmitAnswerRequest answer(Long questionId, Long optionId) {
        SubmitAnswerRequest answer = new SubmitAnswerRequest();
        answer.setQuestionId(questionId);
        answer.setSelectedOptionId(optionId);
        return answer;
    }
}
