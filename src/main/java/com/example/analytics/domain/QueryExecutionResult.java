package com.example.analytics.domain;

import java.util.List;

public record QueryExecutionResult(
        List<AnalyticsRow> rows,
        long executionTimeMs,
        String bigQueryJobId
) {
}
