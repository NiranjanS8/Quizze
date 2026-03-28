package com.quizze.quizze.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class ApplicationMetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public ApplicationMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(String metricName) {
        counters.computeIfAbsent(metricName, name -> Counter.builder(name).register(meterRegistry)).increment();
    }

    public void increment(String metricName, double amount) {
        counters.computeIfAbsent(metricName, name -> Counter.builder(name).register(meterRegistry)).increment(amount);
    }
}
