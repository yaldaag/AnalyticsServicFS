package com.example.analytics.service.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinition.PredicateDefinition;
import com.example.analytics.domain.MetricDefinition.SourceDefinition;
import com.example.analytics.domain.MetricDefinition.ValueDefinition;
import com.example.analytics.domain.MetricDefinitionsDocument;
import com.example.analytics.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricConfigValidatorTest {

    private final MetricConfigValidator validator = new MetricConfigValidator();

    @Test
    void shouldAcceptValidConfiguration() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily_active_users", "dataset.table", "activity_date", null, null, AggregationType.COUNT)));

        assertDoesNotThrow(() -> validator.validate(document));
    }

    @Test
    void shouldRejectDuplicateMetricNames() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(
                metric("daily_active_users", "dataset.table", "activity_date", null, null, AggregationType.COUNT),
                metric("daily_active_users", "dataset.table2", "activity_date", "amount", null, AggregationType.SUM)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldRejectUnsafeIdentifiers() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily users", "dataset.table", "activity_date", null, null, AggregationType.COUNT)));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldRejectSumWithoutValueColumn() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily_revenue", "dataset.table", "activity_date", null, null, AggregationType.SUM)));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldAcceptExpressionBasedAggregation() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric(
                "daily_margin",
                "dataset.table",
                "activity_date",
                null,
                "IFNULL(revenue_amount, 0)",
                AggregationType.SUM
        )));

        assertDoesNotThrow(() -> validator.validate(document));
    }

    @Test
    void shouldRejectColumnAndExpressionTogether() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric(
                "daily_margin",
                "dataset.table",
                "activity_date",
                "revenue_amount",
                "IFNULL(revenue_amount, 0)",
                AggregationType.SUM
        )));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldRejectUnsupportedPredicateOperator() {
        MetricDefinition metric = metric("daily_active_users", "dataset.table", "activity_date", null, null, AggregationType.COUNT);
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setColumn("event_type");
        predicate.setOperator("LIKE");
        predicate.setLiteral("login");
        metric.setPredicates(List.of(predicate));

        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    private MetricDefinition metric(
            String name,
            String table,
            String dateColumn,
            String valueColumn,
            String expression,
            AggregationType aggregationType
    ) {
        MetricDefinition metric = new MetricDefinition();
        metric.setName(name);

        SourceDefinition source = new SourceDefinition();
        source.setTable(table);
        source.setDateColumn(dateColumn);
        source.setTenantColumn("tenant_id");
        source.setMemberColumn("member_id");
        metric.setSource(source);

        ValueDefinition value = new ValueDefinition();
        value.setAggregation(aggregationType);
        value.setColumn(valueColumn);
        value.setExpression(expression);
        metric.setValue(value);

        return metric;
    }
}
