package com.example.analytics.service.config;

import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinition.PredicateDefinition;
import com.example.analytics.domain.MetricDefinition.SourceDefinition;
import com.example.analytics.domain.MetricDefinition.ValueDefinition;
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
    private static final Pattern SAFE_EXPRESSION = Pattern.compile("^[A-Za-z0-9_$.(),\\s+\\-*/]+$");
    private static final Set<String> ALLOWED_OPERATORS = Set.of("=", "!=", ">", ">=", "<", "<=");

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
        validateIdentifier(metric.getName(), "metric name");

        validateSource(metric.getSource());
        validateValue(metric.getValue());
        validatePredicates(metric.getPredicates());
    }

    private void validateSource(SourceDefinition source) {
        if (source == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric source is required");
        }
        require(source.getTable(), "Metric table is required");
        require(source.getDateColumn(), "Metric date column is required");
        require(source.getTenantColumn(), "Metric tenant column is required");
        require(source.getMemberColumn(), "Metric member column is required");

        validateIdentifier(source.getTable(), "metric table");
        validateIdentifier(source.getDateColumn(), "metric date column");
        validateIdentifier(source.getTenantColumn(), "metric tenant column");
        validateIdentifier(source.getMemberColumn(), "metric member column");
    }

    private void validateValue(ValueDefinition value) {
        if (value == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric value definition is required");
        }
        if (value.getAggregation() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric aggregation type is required");
        }

        boolean hasColumn = value.getColumn() != null && !value.getColumn().isBlank();
        boolean hasExpression = value.getExpression() != null && !value.getExpression().isBlank();

        if (hasColumn) {
            validateIdentifier(value.getColumn(), "metric value column");
        }
        if (hasExpression) {
            validateExpression(value.getExpression(), "metric value expression");
        }

        if (value.getAggregation().requiresArgument() && hasColumn == hasExpression) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric value must define exactly one of column or expression for aggregation " + value.getAggregation());
        }

        if (!value.getAggregation().requiresArgument() && (hasColumn || hasExpression)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Metric aggregation " + value.getAggregation() + " does not support value column or expression");
        }
    }

    private void validatePredicates(List<PredicateDefinition> predicates) {
        if (predicates == null) {
            return;
        }
        for (PredicateDefinition predicate : predicates) {
            require(predicate.getColumn(), "Metric predicate column is required");
            require(predicate.getOperator(), "Metric predicate operator is required");
            if (predicate.getLiteral() == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                        "Metric predicate literal is required");
            }

            validateIdentifier(predicate.getColumn(), "metric predicate column");
            if (!ALLOWED_OPERATORS.contains(predicate.getOperator())) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                        "Unsupported metric predicate operator: " + predicate.getOperator());
            }
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

    private void validateExpression(String value, String fieldName) {
        if (!SAFE_EXPRESSION.matcher(value).matches()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                    "Unsafe " + fieldName + ": " + value);
        }
    }
}
