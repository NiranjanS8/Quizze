package com.quizze.quizze.cache.service;

import static com.quizze.quizze.cache.config.CacheConfig.ADMIN_OVERVIEW_CACHE;
import static com.quizze.quizze.cache.config.CacheConfig.QUIZ_ANALYTICS_CACHE;
import static com.quizze.quizze.cache.config.CacheConfig.QUIZ_LEADERBOARD_CACHE;
import static com.quizze.quizze.cache.config.CacheConfig.QUESTION_ANALYTICS_CACHE;
import static com.quizze.quizze.cache.config.CacheConfig.USER_PERFORMANCE_CACHE;

import org.springframework.cache.annotation.Caching;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuizCacheInvalidationService {

    @Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUIZ_ANALYTICS_CACHE, key = "#quizId"),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUESTION_ANALYTICS_CACHE, key = "#quizId"),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = ADMIN_OVERVIEW_CACHE, allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUIZ_LEADERBOARD_CACHE, allEntries = true)
    })
    public void evictAnalyticsForQuiz(Long quizId) {
        log.debug("Evicted quiz-related caches for quizId={}", quizId);
    }

    @Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUIZ_ANALYTICS_CACHE, key = "#quizId"),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUESTION_ANALYTICS_CACHE, key = "#quizId"),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = QUIZ_LEADERBOARD_CACHE, allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = USER_PERFORMANCE_CACHE, key = "#userId"),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = ADMIN_OVERVIEW_CACHE, allEntries = true)
    })
    public void evictAfterQuizSubmission(Long quizId, Long userId) {
        log.debug("Evicted submission-related caches for quizId={} and userId={}", quizId, userId);
    }
}
