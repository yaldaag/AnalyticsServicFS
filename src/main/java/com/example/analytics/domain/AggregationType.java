package com.example.analytics.domain;

public enum AggregationType {
    COUNT,
    SUM,
    AVG;

    public boolean requiresArgument() {
        return this == SUM || this == AVG;
    }
}
