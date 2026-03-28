package com.quizze.quizze.cache.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private long quizLeaderboardTtlMinutes = 10;
    private long quizAnalyticsTtlMinutes = 15;
    private long questionAnalyticsTtlMinutes = 15;
    private long adminOverviewTtlMinutes = 10;
    private long userPerformanceTtlMinutes = 10;
}
