package com.quizze.quizze.quiz.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.repository.QuizRepository;
import com.quizze.quizze.security.jwt.JwtService;
import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.RoleRepository;
import com.quizze.quizze.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserQuizControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private JwtService jwtService;

    private String bearerToken;
    private Quiz publishedQuiz;
    private Question savedQuestion;
    private Option correctOption;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleType.USER).orElseThrow();

        User user = new User();
        user.setFirstName("Quiz");
        user.setLastName("User");
        user.setEmail("quiz-user@example.com");
        user.setUsername("quiz-user");
        user.setPassword("encoded");
        user.setRole(userRole);
        User savedUser = userRepository.save(user);
        bearerToken = "Bearer " + jwtService.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole().getName().name());

        Quiz quiz = new Quiz();
        quiz.setTitle("Java Basics");
        quiz.setDescription("Core Java questions");
        quiz.setDifficulty(DifficultyLevel.EASY);
        quiz.setTimeLimitInMinutes(15);
        quiz.setPublished(true);

        Question question = new Question();
        question.setContent("Which keyword is used to inherit a class in Java?");
        question.setPoints(5);
        question.setQuiz(quiz);

        Option optionA = new Option();
        optionA.setContent("extends");
        optionA.setCorrect(true);
        optionA.setQuestion(question);

        Option optionB = new Option();
        optionB.setContent("implements");
        optionB.setCorrect(false);
        optionB.setQuestion(question);

        question.getOptions().add(optionA);
        question.getOptions().add(optionB);
        quiz.getQuestions().add(question);

        publishedQuiz = quizRepository.save(quiz);
        savedQuestion = publishedQuiz.getQuestions().get(0);
        correctOption = savedQuestion.getOptions().stream().filter(Option::isCorrect).findFirst().orElseThrow();
    }

    @Test
    void getPublishedQuizzesShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPublishedQuizzesShouldReturnCatalogForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Java Basics"));
    }

    @Test
    void startAndSubmitQuizShouldReturnEvaluatedResult() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/quizzes/{id}/start", publishedQuiz.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quizId").value(publishedQuiz.getId()))
                .andReturn();

        JsonNode startJson = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long attemptId = startJson.path("data").path("attemptId").asLong();

        String submitPayload = """
                {
                  "answers": [
                    {
                      "questionId": %d,
                      "selectedOptionId": %d
                    }
                  ]
                }
                """.formatted(savedQuestion.getId(), correctOption.getId());

        mockMvc.perform(post("/api/quizzes/attempts/{attemptId}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correctAnswers").value(1))
                .andExpect(jsonPath("$.data.wrongAnswers").value(0))
                .andExpect(jsonPath("$.data.score").value(5.0))
                .andExpect(jsonPath("$.data.percentage").value(100.0));
    }
}
