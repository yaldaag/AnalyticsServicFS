package com.example.analytics.service.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinitionsDocument;
import com.example.analytics.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricConfigValidatorTest {

    private final MetricConfigValidator validator = new MetricConfigValidator();

    @Test
    void shouldAcceptValidConfiguration() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily_active_users", "dataset.table", "activity_date", null, AggregationType.COUNT)));

        assertDoesNotThrow(() -> validator.validate(document));
    }

    @Test
    void shouldRejectDuplicateMetricNames() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(
                metric("daily_active_users", "dataset.table", "activity_date", null, AggregationType.COUNT),
                metric("daily_active_users", "dataset.table2", "activity_date", "amount", AggregationType.SUM)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldRejectUnsafeIdentifiers() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily users", "dataset.table", "activity_date", null, AggregationType.COUNT)));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    @Test
    void shouldRejectSumWithoutValueColumn() {
        MetricDefinitionsDocument document = new MetricDefinitionsDocument();
        document.setMetrics(List.of(metric("daily_revenue", "dataset.table", "activity_date", null, AggregationType.SUM)));

        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(document));

        assertEquals("INVALID_METRIC_CONFIGURATION", exception.getErrorCode());
    }

    private MetricDefinition metric(
            String name,
            String table,
            String dateColumn,
            String valueColumn,
            AggregationType aggregationType
    ) {
        MetricDefinition metric = new MetricDefinition();
        metric.setName(name);
        metric.setTable(table);
        metric.setDateColumn(dateColumn);
        metric.setValueColumn(valueColumn);
        metric.setTenantColumn("tenant_id");
        metric.setMemberColumn("member_id");
        metric.setAggregationType(aggregationType);
        return metric;
    }
}
