package com.example.analytics.repository;

import com.example.analytics.domain.QueryExecutionResult;
import com.example.analytics.service.query.BuiltQuery;

public interface AnalyticsRepository {

    QueryExecutionResult execute(BuiltQuery query);
}
