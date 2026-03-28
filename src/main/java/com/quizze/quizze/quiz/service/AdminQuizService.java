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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AdminQuizMapper adminQuizMapper;

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        log.info("Creating quiz with title='{}' and category='{}'", request.getTitle(), request.getCategoryName());
        Quiz quiz = new Quiz();
        applyQuizDetails(quiz, request);
        Quiz savedQuiz = quizRepository.save(quiz);
        log.info("Quiz created successfully with quizId={}", savedQuiz.getId());
        return adminQuizMapper.toQuizResponse(savedQuiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getAllQuizzes() {
        log.debug("Fetching all quizzes for admin management");
        return quizRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Quiz::getCreatedAt).reversed())
                .map(adminQuizMapper::toQuizResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {
        log.debug("Fetching quiz details for quizId={}", quizId);
        return adminQuizMapper.toQuizResponse(getQuizEntity(quizId));
    }

    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizRequest request) {
        log.info("Updating quizId={} with title='{}'", quizId, request.getTitle());
        Quiz quiz = getQuizEntity(quizId);
        applyQuizDetails(quiz, request);
        log.info("Quiz updated successfully for quizId={}", quizId);
        return adminQuizMapper.toQuizResponse(quiz);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        log.info("Deleting quiz with quizId={}", quizId);
        Quiz quiz = getQuizEntity(quizId);
        quizRepository.delete(quiz);
        log.info("Quiz deleted successfully for quizId={}", quizId);
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        log.info("Adding question to quizId={} with content='{}'", quizId, request.getContent());
        Quiz quiz = getQuizEntity(quizId);
        validateQuestionOptions(request);

        Question question = new Question();
        question.setQuiz(quiz);
        applyQuestionDetails(question, request);
        Question savedQuestion = questionRepository.save(question);
        log.info("Question added successfully with questionId={} to quizId={}", savedQuestion.getId(), quizId);

        return adminQuizMapper.toQuestionResponse(savedQuestion);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        log.info("Updating questionId={} with content='{}'", questionId, request.getContent());
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        validateQuestionOptions(request);
        applyQuestionDetails(question, request);
        log.info("Question updated successfully for questionId={}", questionId);

        return adminQuizMapper.toQuestionResponse(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        log.info("Deleting question with questionId={}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        questionRepository.delete(question);
        log.info("Question deleted successfully for questionId={}", questionId);
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
