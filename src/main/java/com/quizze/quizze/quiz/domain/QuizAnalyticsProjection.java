package com.quizze.quizze.quiz.domain;

import com.quizze.quizze.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_analytics_projections")
public class QuizAnalyticsProjection extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long quizId;

    @Column(nullable = false, length = 180)
    private String quizTitle;

    @Column(length = 120)
    private String categoryName;

    @Column(nullable = false)
    private long submittedAttempts = 0;

    @Column(nullable = false)
    private double averageScore = 0.0;

    @Column(nullable = false)
    private double averagePercentage = 0.0;

    @Column(nullable = false)
    private double highestScore = 0.0;

    @Column(nullable = false)
    private double lowestScore = 0.0;

    @Column(nullable = false)
    private double maxScore = 0.0;

    @Column(nullable = false)
    private double averageCorrectAnswers = 0.0;

    @Column(nullable = false)
    private double averageWrongAnswers = 0.0;

    private LocalDateTime lastSubmittedAt;
}
