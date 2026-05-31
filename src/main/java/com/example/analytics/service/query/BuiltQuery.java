package com.example.analytics.service.query;

import com.google.cloud.bigquery.QueryJobConfiguration;

public record BuiltQuery(String sql, QueryJobConfiguration queryConfiguration) {
}
