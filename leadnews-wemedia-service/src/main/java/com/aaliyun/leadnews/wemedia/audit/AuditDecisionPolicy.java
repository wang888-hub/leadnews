package com.aaliyun.leadnews.wemedia.audit;

import com.aaliyun.leadnews.model.ai.*;
import com.aaliyun.leadnews.wemedia.config.AuditProperties;
import org.springframework.stereotype.Component;

@Component
public class AuditDecisionPolicy {

    private final AuditProperties p;

    public AuditDecisionPolicy(AuditProperties p) {
        this.p = p;
    }

    public Outcome decide(ArticleAuditResult r) {
        return switch (r.decision()) {
            case PASS -> r.confidence() >= p.ai().autoPassConfidenceThreshold()
                    ? Outcome.APPROVE
                    : Outcome.MANUAL_REVIEW;
            case REJECT -> r.confidence() >= p.ai().autoRejectConfidenceThreshold()
                    ? Outcome.REJECT
                    : Outcome.MANUAL_REVIEW;
            case REVIEW -> Outcome.MANUAL_REVIEW;
        };
    }

    public enum Outcome {
        APPROVE,
        REJECT,
        MANUAL_REVIEW
    }
}