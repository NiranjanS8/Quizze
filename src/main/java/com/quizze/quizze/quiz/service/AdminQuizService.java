package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.Category;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.dto.admin.OptionRequest;
import com.quizze.quizze.quiz.dto.admin.OptionResponse;
import com.quizze.quizze.quiz.dto.admin.QuestionRequest;
import com.quizze.quizze.quiz.dto.admin.QuestionResponse;
import com.quizze.quizze.quiz.dto.admin.QuizRequest;
import com.quizze.quizze.quiz.dto.admin.QuizResponse;
import com.quizze.quizze.quiz.repository.CategoryRepository;
import com.quizze.quizze.quiz.repository.QuestionRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Quiz quiz = new Quiz();
        applyQuizDetails(quiz, request);
        Quiz savedQuiz = quizRepository.save(quiz);
        return mapQuizResponse(savedQuiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getAllQuizzes() {
        return quizRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Quiz::getCreatedAt).reversed())
                .map(this::mapQuizResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {
        return mapQuizResponse(getQuizEntity(quizId));
    }

    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = getQuizEntity(quizId);
        applyQuizDetails(quiz, request);
        return mapQuizResponse(quiz);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = getQuizEntity(quizId);
        quizRepository.delete(quiz);
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = getQuizEntity(quizId);
        validateQuestionOptions(request);

        Question question = new Question();
        question.setQuiz(quiz);
        applyQuestionDetails(question, request);
        Question savedQuestion = questionRepository.save(question);

        return mapQuestionResponse(savedQuestion);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        validateQuestionOptions(request);
        applyQuestionDetails(question, request);

        return mapQuestionResponse(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        questionRepository.delete(question);
    }

    private Quiz getQuizEntity(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
    }

    private void applyQuizDetails(Quiz quiz, QuizRequest request) {
        quiz.setTitle(request.getTitle().trim());
        quiz.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        quiz.setCategory(resolveCategory(request.getCategoryName()));
        quiz.setDifficulty(request.getDifficulty());
        quiz.setTimeLimitInMinutes(request.getTimeLimitInMinutes());
        quiz.setPublished(request.isPublished());
        quiz.setNegativeMarkingEnabled(request.isNegativeMarkingEnabled());
        quiz.setOneAttemptOnly(request.isOneAttemptOnly());
    }

    private Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }

        String normalizedName = categoryName.trim();
        return categoryRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(normalizedName);
                    return categoryRepository.save(category);
                });
    }

    private void applyQuestionDetails(Question question, QuestionRequest request) {
        question.setContent(request.getContent().trim());
        question.setPoints(request.getPoints());

        question.getOptions().clear();
        for (OptionRequest optionRequest : request.getOptions()) {
            Option option = new Option();
            option.setQuestion(question);
            option.setContent(optionRequest.getContent().trim());
            option.setCorrect(optionRequest.isCorrect());
            question.getOptions().add(option);
        }
    }

    private void validateQuestionOptions(QuestionRequest request) {
        long correctOptionCount = request.getOptions()
                .stream()
                .filter(OptionRequest::isCorrect)
                .count();

        if (correctOptionCount != 1) {
            throw new BadRequestException("A question must have exactly one correct option");
        }
    }

    private QuizResponse mapQuizResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions()
                .stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(this::mapQuestionResponse)
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

    private QuestionResponse mapQuestionResponse(Question question) {
        List<OptionResponse> options = question.getOptions()
                .stream()
                .sorted(Comparator.comparing(Option::getId))
                .map(option -> OptionResponse.builder()
                        .id(option.getId())
                        .content(option.getContent())
                        .correct(option.isCorrect())
                        .build())
                .toList();

        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .points(question.getPoints())
                .options(options)
                .build();
    }
}
