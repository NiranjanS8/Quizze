package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptAnswer;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.dto.analytics.QuestionAnalyticsItemResponse;
import com.quizze.quizze.quiz.dto.analytics.QuestionAnalyticsResponse;
import com.quizze.quizze.quiz.repository.AttemptAnswerRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionAnalyticsService {

    private final QuizRepository quizRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    @Transactional(readOnly = true)
    public QuestionAnalyticsResponse getQuestionAnalytics(Long quizId) {
        log.debug("Fetching question analytics for quizId={}", quizId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<AttemptAnswer> attemptAnswers = attemptAnswerRepository.findByQuestionQuizId(quizId);

        List<QuestionAnalyticsItemResponse> items = quiz.getQuestions().stream()
                .map(question -> buildQuestionAnalytics(question, attemptAnswers))
                .filter(item -> item.getTotalAnswers() > 0)
                .toList();

        List<QuestionAnalyticsItemResponse> hardestQuestions = items.stream()
                .sorted(Comparator.comparing(QuestionAnalyticsItemResponse::getCorrectPercentage)
                        .thenComparing(QuestionAnalyticsItemResponse::getTotalAnswers, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        List<QuestionAnalyticsItemResponse> easiestQuestions = items.stream()
                .sorted(Comparator.comparing(QuestionAnalyticsItemResponse::getCorrectPercentage, Comparator.reverseOrder())
                        .thenComparing(QuestionAnalyticsItemResponse::getTotalAnswers, Comparator.reverseOrder()))
                .limit(5)
                .toList();
        log.debug("Question analytics computed for quizId={} with {} analyzed questions", quizId, items.size());

        return QuestionAnalyticsResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .hardestQuestions(hardestQuestions)
                .easiestQuestions(easiestQuestions)
                .build();
    }

    private QuestionAnalyticsItemResponse buildQuestionAnalytics(Question question, List<AttemptAnswer> allAnswers) {
        List<AttemptAnswer> answers = allAnswers.stream()
                .filter(answer -> answer.getQuestion().getId().equals(question.getId()))
                .toList();

        long totalAnswers = answers.size();
        long correctAnswers = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        long wrongAnswers = totalAnswers - correctAnswers;
        double correctPercentage = totalAnswers == 0 ? 0.0 : (correctAnswers * 100.0) / totalAnswers;
        double wrongPercentage = totalAnswers == 0 ? 0.0 : (wrongAnswers * 100.0) / totalAnswers;

        return QuestionAnalyticsItemResponse.builder()
                .questionId(question.getId())
                .questionContent(question.getContent())
                .points(question.getPoints())
                .totalAnswers(totalAnswers)
                .correctAnswers(correctAnswers)
                .wrongAnswers(wrongAnswers)
                .correctPercentage(correctPercentage)
                .wrongPercentage(wrongPercentage)
                .build();
    }
}
