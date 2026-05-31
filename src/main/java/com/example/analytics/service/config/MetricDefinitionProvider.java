package com.example.analytics.service.config;

import com.example.analytics.domain.MetricDefinition;

public interface MetricDefinitionProvider {

    MetricDefinition getRequiredMetric(String metricName);
}
