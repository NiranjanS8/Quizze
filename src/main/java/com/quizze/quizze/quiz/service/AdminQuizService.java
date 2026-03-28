package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.Category;
import com.quizze.quizze.quiz.domain.Option;
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
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AdminQuizMapper adminQuizMapper;

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Quiz quiz = new Quiz();
        applyQuizDetails(quiz, request);
        Quiz savedQuiz = quizRepository.save(quiz);
        return adminQuizMapper.toQuizResponse(savedQuiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getAllQuizzes() {
        return quizRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Quiz::getCreatedAt).reversed())
                .map(adminQuizMapper::toQuizResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {
        return adminQuizMapper.toQuizResponse(getQuizEntity(quizId));
    }

    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = getQuizEntity(quizId);
        applyQuizDetails(quiz, request);
        return adminQuizMapper.toQuizResponse(quiz);
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

        return adminQuizMapper.toQuestionResponse(savedQuestion);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        validateQuestionOptions(request);
        applyQuestionDetails(question, request);

        return adminQuizMapper.toQuestionResponse(question);
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

        Set<String> normalizedOptions = request.getOptions().stream()
                .map(option -> option.getContent().trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        if (correctOptionCount != 1) {
            throw new BadRequestException("A question must have exactly one correct option");
        }

        if (normalizedOptions.size() != request.getOptions().size()) {
            throw new BadRequestException("Question options must be unique");
        }
    }
}
