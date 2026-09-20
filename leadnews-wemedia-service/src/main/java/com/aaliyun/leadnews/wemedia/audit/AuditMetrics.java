package com.aaliyun.leadnews.wemedia.audit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuditMetrics {

    private final MeterRegistry registry;

    public AuditMetrics(MeterRegistry r) {
        registry = r;
    }

    public void increment(String name) {
        registry.counter(name).increment();
    }

    public void aiLatency(long millis) {
        registry.timer("audit.ai.latency").record(Duration.ofMillis(millis));
    }
}