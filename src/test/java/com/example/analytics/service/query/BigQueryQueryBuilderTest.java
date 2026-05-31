package com.example.analytics.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinition.PredicateDefinition;
import com.example.analytics.domain.MetricDefinition.SourceDefinition;
import com.example.analytics.domain.MetricDefinition.ValueDefinition;
import com.example.analytics.domain.RequestContext;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import java.time.LocalDate;
import java.util.List;
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

        MetricDefinition metricDefinition = metric(
                "daily_active_users",
                "demo.analytics.daily_active_users",
                "activity_date",
                AggregationType.COUNT,
                null,
                null,
                null
        );

        BuiltQuery builtQuery = queryBuilder.build(request, new RequestContext("wix", "member-1"), metricDefinition);

        assertTrue(builtQuery.sql().contains("FROM `demo.analytics.daily_active_users`"));
        assertTrue(builtQuery.sql().contains("COUNT(1) AS value"));
        assertEquals("2024-01-01", builtQuery.queryConfiguration().getNamedParameters().get("dateStart").getValue());
        assertEquals("wix", builtQuery.queryConfiguration().getNamedParameters().get("tenantId").getValue());
        assertEquals(1024L, builtQuery.queryConfiguration().getMaximumBytesBilled());
    }

    @Test
    void shouldBuildParameterizedSumQueryWithStaticPredicate() {
        AnalyticsProperties properties = new AnalyticsProperties();
        BigQueryQueryBuilder queryBuilder = new BigQueryQueryBuilder(properties);

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setMetricName("daily_revenue");
        request.setDateStart(LocalDate.parse("2024-01-01"));
        request.setDateEnd(LocalDate.parse("2024-01-31"));

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setColumn("region");
        predicate.setOperator("=");
        predicate.setLiteral("emea");

        MetricDefinition metricDefinition = metric(
                "daily_revenue",
                "demo.analytics.daily_revenue",
                "revenue_date",
                AggregationType.SUM,
                "revenue_amount",
                null,
                List.of(predicate)
        );

        BuiltQuery builtQuery = queryBuilder.build(request, new RequestContext("wix", "member-1"), metricDefinition);

        assertTrue(builtQuery.sql().contains("SUM(revenue_amount) AS value"));
        assertTrue(builtQuery.sql().contains("region = @configPredicate0"));
        assertEquals("emea", builtQuery.queryConfiguration().getNamedParameters().get("configPredicate0").getValue());
    }

    @Test
    void shouldBuildExpressionBasedAverageQuery() {
        AnalyticsProperties properties = new AnalyticsProperties();
        BigQueryQueryBuilder queryBuilder = new BigQueryQueryBuilder(properties);

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setMetricName("daily_margin");
        request.setDateStart(LocalDate.parse("2024-01-01"));
        request.setDateEnd(LocalDate.parse("2024-01-31"));

        MetricDefinition metricDefinition = metric(
                "daily_margin",
                "demo.analytics.daily_margin",
                "metric_date",
                AggregationType.AVG,
                null,
                "IFNULL(margin_amount, 0)",
                null
        );

        BuiltQuery builtQuery = queryBuilder.build(request, new RequestContext("wix", "member-1"), metricDefinition);

        assertTrue(builtQuery.sql().contains("AVG(IFNULL(margin_amount, 0)) AS value"));
    }

    private MetricDefinition metric(
            String name,
            String table,
            String dateColumn,
            AggregationType aggregationType,
            String valueColumn,
            String expression,
            List<PredicateDefinition> predicates
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
        metric.setPredicates(predicates);
        return metric;
    }
}
