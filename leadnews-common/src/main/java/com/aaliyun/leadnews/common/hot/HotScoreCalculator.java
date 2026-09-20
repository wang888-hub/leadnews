package com.aaliyun.leadnews.common.hot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class HotScoreCalculator {
    private final double viewWeight, likeWeight, commentWeight, collectWeight, decayExponent, ageOffsetHours;
    public HotScoreCalculator(double viewWeight, double likeWeight, double commentWeight,
                              double collectWeight, double decayExponent, double ageOffsetHours) {
        if (viewWeight < 0 || likeWeight < 0 || commentWeight < 0 || collectWeight < 0 || decayExponent < 0 || ageOffsetHours <= 0)
            throw new IllegalArgumentException("Invalid hot-score parameters");
        this.viewWeight=viewWeight; this.likeWeight=likeWeight; this.commentWeight=commentWeight;
        this.collectWeight=collectWeight; this.decayExponent=decayExponent; this.ageOffsetHours=ageOffsetHours;
    }
    public double calculate(long views,long likes,long comments,long collects,Instant publishedAt,Instant now) {
        Objects.requireNonNull(publishedAt); Objects.requireNonNull(now);
        double hours=Math.max(0D,Duration.between(publishedAt,now).toMillis()/3_600_000D);
        double behavior=Math.max(0,views)*viewWeight+Math.max(0,likes)*likeWeight+Math.max(0,comments)*commentWeight+Math.max(0,collects)*collectWeight;
        return (1D+behavior)/Math.pow(hours+ageOffsetHours,decayExponent);
    }
}
