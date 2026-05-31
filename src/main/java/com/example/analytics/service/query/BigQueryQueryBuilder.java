package com.example.analytics.service.query;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.RequestContext;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Component;

@Component
public class BigQueryQueryBuilder {

    private final AnalyticsProperties analyticsProperties;

    public BigQueryQueryBuilder(AnalyticsProperties analyticsProperties) {
        this.analyticsProperties = analyticsProperties;
    }

    public BuiltQuery build(AnalyticsQueryRequest request, RequestContext context, MetricDefinition metricDefinition) {
        String valueExpression = buildValueExpression(metricDefinition);
        String sql = """
                SELECT
                    %s AS date,
                    %s AS value
                FROM `%s`
                WHERE %s BETWEEN @dateStart AND @dateEnd
                  AND %s = @tenantId
                  AND %s = @memberId
                GROUP BY date
                ORDER BY date ASC
                """.formatted(
                metricDefinition.getDateColumn(),
                valueExpression,
                metricDefinition.getTable(),
                metricDefinition.getDateColumn(),
                metricDefinition.getTenantColumn(),
                metricDefinition.getMemberColumn()
        );

        QueryJobConfiguration.Builder builder = QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("dateStart", QueryParameterValue.date(request.getDateStart().toString()))
                .addNamedParameter("dateEnd", QueryParameterValue.date(request.getDateEnd().toString()))
                .addNamedParameter("tenantId", QueryParameterValue.string(context.tenantId()))
                .addNamedParameter("memberId", QueryParameterValue.string(context.memberId()))
                .setUseLegacySql(false);

        if (analyticsProperties.getMaxBytesBilled() != null) {
            builder.setMaximumBytesBilled(analyticsProperties.getMaxBytesBilled());
        }

        return new BuiltQuery(sql, builder.build());
    }

    private String buildValueExpression(MetricDefinition metricDefinition) {
        AggregationType aggregationType = metricDefinition.getAggregationType();
        return switch (aggregationType) {
            case COUNT -> "COUNT(1)";
            case SUM -> "SUM(%s)".formatted(metricDefinition.getValueColumn());
            case AVG -> "AVG(%s)".formatted(metricDefinition.getValueColumn());
        };
    }
}
