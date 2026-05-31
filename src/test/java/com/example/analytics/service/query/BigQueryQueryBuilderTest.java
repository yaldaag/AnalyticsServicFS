package com.example.analytics.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.RequestContext;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BigQueryQueryBuilderTest {

    @Test
    void shouldBuildParameterizedCountQuery() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setMaxBytesBilled(1024L);
        BigQueryQueryBuilder queryBuilder = new BigQueryQueryBuilder(properties);

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setMetricName("daily_active_users");
        request.setDateStart(LocalDate.parse("2024-01-01"));
        request.setDateEnd(LocalDate.parse("2024-01-31"));

        MetricDefinition metricDefinition = new MetricDefinition();
        metricDefinition.setName("daily_active_users");
        metricDefinition.setTable("demo.analytics.daily_active_users");
        metricDefinition.setDateColumn("activity_date");
        metricDefinition.setTenantColumn("tenant_id");
        metricDefinition.setMemberColumn("member_id");
        metricDefinition.setAggregationType(AggregationType.COUNT);

        BuiltQuery builtQuery = queryBuilder.build(request, new RequestContext("wix", "member-1"), metricDefinition);

        assertTrue(builtQuery.sql().contains("FROM `demo.analytics.daily_active_users`"));
        assertTrue(builtQuery.sql().contains("COUNT(1) AS value"));
        assertEquals("2024-01-01", builtQuery.queryConfiguration().getNamedParameters().get("dateStart").getValue());
        assertEquals("wix", builtQuery.queryConfiguration().getNamedParameters().get("tenantId").getValue());
        assertEquals(1024L, builtQuery.queryConfiguration().getMaximumBytesBilled());
    }
}
