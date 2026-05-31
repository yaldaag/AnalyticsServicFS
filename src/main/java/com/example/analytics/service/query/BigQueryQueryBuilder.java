package com.example.analytics.service.query;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.AggregationType;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinition.PredicateDefinition;
import com.example.analytics.domain.MetricDefinition.SourceDefinition;
import com.example.analytics.domain.MetricDefinition.ValueDefinition;
import com.example.analytics.domain.RequestContext;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BigQueryQueryBuilder {

    private final AnalyticsProperties analyticsProperties;

    public BigQueryQueryBuilder(AnalyticsProperties analyticsProperties) {
        this.analyticsProperties = analyticsProperties;
    }

    public BuiltQuery build(AnalyticsQueryRequest request, RequestContext context, MetricDefinition metricDefinition) {
        SourceDefinition source = metricDefinition.getSource();
        ValueDefinition value = metricDefinition.getValue();
        List<String> filters = new ArrayList<>();
        filters.add("%s BETWEEN @dateStart AND @dateEnd".formatted(source.getDateColumn()));
        filters.add("%s = @tenantId".formatted(source.getTenantColumn()));
        filters.add("%s = @memberId".formatted(source.getMemberColumn()));
        filters.addAll(buildStaticPredicates(metricDefinition.getPredicates()));

        String sql = """
                SELECT
                    %s AS date,
                    %s AS value
                FROM `%s`
                WHERE %s
                GROUP BY date
                ORDER BY date ASC
                """.formatted(
                source.getDateColumn(),
                buildValueExpression(value),
                source.getTable(),
                String.join("\n  AND ", filters)
        );

        QueryJobConfiguration.Builder builder = QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("dateStart", QueryParameterValue.date(request.getDateStart().toString()))
                .addNamedParameter("dateEnd", QueryParameterValue.date(request.getDateEnd().toString()))
                .addNamedParameter("tenantId", QueryParameterValue.string(context.tenantId()))
                .addNamedParameter("memberId", QueryParameterValue.string(context.memberId()))
                .setUseLegacySql(false);
        addStaticPredicateParameters(builder, metricDefinition.getPredicates());

        if (analyticsProperties.getMaxBytesBilled() != null) {
            builder.setMaximumBytesBilled(analyticsProperties.getMaxBytesBilled());
        }

        return new BuiltQuery(sql, builder.build());
    }

    private String buildValueExpression(ValueDefinition value) {
        AggregationType aggregationType = value.getAggregation();
        String argument = value.getExpression() != null && !value.getExpression().isBlank()
                ? value.getExpression()
                : value.getColumn();
        return switch (aggregationType) {
            case COUNT -> "COUNT(1)";
            case SUM -> "SUM(%s)".formatted(argument);
            case AVG -> "AVG(%s)".formatted(argument);
        };
    }

    private List<String> buildStaticPredicates(List<PredicateDefinition> predicates) {
        List<String> clauses = new ArrayList<>();
        if (predicates == null) {
            return clauses;
        }
        for (int index = 0; index < predicates.size(); index++) {
            PredicateDefinition predicate = predicates.get(index);
            clauses.add("%s %s @configPredicate%d".formatted(predicate.getColumn(), predicate.getOperator(), index));
        }
        return clauses;
    }

    private void addStaticPredicateParameters(
            QueryJobConfiguration.Builder builder,
            List<PredicateDefinition> predicates
    ) {
        if (predicates == null) {
            return;
        }
        for (int index = 0; index < predicates.size(); index++) {
            builder.addNamedParameter("configPredicate" + index, QueryParameterValue.string(predicates.get(index).getLiteral()));
        }
    }
}
