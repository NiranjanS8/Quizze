package com.quizze.quizze.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    public static final String QUIZ_LEADERBOARD_CACHE = "quizLeaderboard";
    public static final String QUIZ_ANALYTICS_CACHE = "quizAnalytics";
    public static final String QUESTION_ANALYTICS_CACHE = "questionAnalytics";
    public static final String ADMIN_OVERVIEW_CACHE = "adminOverview";
    public static final String USER_PERFORMANCE_CACHE = "userPerformance";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, CacheProperties cacheProperties) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper())
                        )
                );

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(
                QUIZ_LEADERBOARD_CACHE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(cacheProperties.getQuizLeaderboardTtlMinutes()))
        );
        cacheConfigurations.put(
                QUIZ_ANALYTICS_CACHE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(cacheProperties.getQuizAnalyticsTtlMinutes()))
        );
        cacheConfigurations.put(
                QUESTION_ANALYTICS_CACHE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(cacheProperties.getQuestionAnalyticsTtlMinutes()))
        );
        cacheConfigurations.put(
                ADMIN_OVERVIEW_CACHE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(cacheProperties.getAdminOverviewTtlMinutes()))
        );
        cacheConfigurations.put(
                USER_PERFORMANCE_CACHE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(cacheProperties.getUserPerformanceTtlMinutes()))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }
}
