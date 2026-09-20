package com.aaliyun.leadnews.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("leadnews.ai.business")
public record AiBusinessProperties(Summary summary, Continuation continuation) {

    public record Summary(int minLength, int maxLength, int maxInputLength) {
    }

    public record Continuation(int minTargetLength,
                               int maxTargetLength,
                               int maxInputLength,
                               int maxInstructionLength) {
    }
}