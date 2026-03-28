package com.quizze.quizze.quiz.mapper;

import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.dto.admin.OptionResponse;
import com.quizze.quizze.quiz.dto.admin.QuestionResponse;
import com.quizze.quizze.quiz.dto.admin.QuizResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminQuizMapper {

    public QuizResponse toQuizResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions()
                .stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(this::toQuestionResponse)
                .toList();

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .categoryName(quiz.getCategory() == null ? null : quiz.getCategory().getName())
                .difficulty(quiz.getDifficulty())
                .timeLimitInMinutes(quiz.getTimeLimitInMinutes())
                .published(quiz.isPublished())
                .negativeMarkingEnabled(quiz.isNegativeMarkingEnabled())
                .oneAttemptOnly(quiz.isOneAttemptOnly())
                .questions(questions)
                .build();
    }

    public QuestionResponse toQuestionResponse(Question question) {
        List<OptionResponse> options = question.getOptions()
                .stream()
                .sorted(Comparator.comparing(Option::getId))
                .map(this::toOptionResponse)
                .toList();

        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .points(question.getPoints())
                .options(options)
                .build();
    }

    private OptionResponse toOptionResponse(Option option) {
        return OptionResponse.builder()
                .id(option.getId())
                .content(option.getContent())
                .correct(option.isCorrect())
                .build();
    }
}
