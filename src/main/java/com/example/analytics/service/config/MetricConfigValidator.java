package com.example.analytics.service.config;

import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinitionsDocument;
import com.example.analytics.web.ApiException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MetricConfigValidator {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$.]+$");

    public void validate(MetricDefinitionsDocument document) {
        List<MetricDefinition> metrics = document.getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric configuration must define at least one metric");
        }

        Set<String> names = new HashSet<>();
        for (MetricDefinition metric : metrics) {
            validateMetric(metric);
            if (!names.add(metric.getName())) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                        "Duplicate metric name: " + metric.getName());
            }
        }
    }

    private void validateMetric(MetricDefinition metric) {
        require(metric.getName(), "Metric name is required");
        require(metric.getTable(), "Metric table is required");
        require(metric.getDateColumn(), "Metric date column is required");
        require(metric.getTenantColumn(), "Metric tenant column is required");
        require(metric.getMemberColumn(), "Metric member column is required");
        if (metric.getAggregationType() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric aggregation type is required");
        }

        validateIdentifier(metric.getName(), "metric name");
        validateIdentifier(metric.getTable(), "metric table");
        validateIdentifier(metric.getDateColumn(), "metric date column");
        validateIdentifier(metric.getTenantColumn(), "metric tenant column");
        validateIdentifier(metric.getMemberColumn(), "metric member column");

        if (metric.getAggregationType() != null && metric.getAggregationType().name().matches("SUM|AVG")) {
            require(metric.getValueColumn(), "Metric value column is required for aggregation " + metric.getAggregationType());
            validateIdentifier(metric.getValueColumn(), "metric value column");
        } else if (metric.getValueColumn() != null && !metric.getValueColumn().isBlank()) {
            validateIdentifier(metric.getValueColumn(), "metric value column");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION", message);
        }
    }

    private void validateIdentifier(String value, String fieldName) {
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Unsafe " + fieldName + ": " + value);
        }
    }
}
