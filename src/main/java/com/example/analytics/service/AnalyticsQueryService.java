package com.example.analytics.service;

import com.example.analytics.cache.CachedPage;
import com.example.analytics.cache.PageTokenService;
import com.example.analytics.cache.ResultPageCache;
import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.AnalyticsRow;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.QueryExecutionResult;
import com.example.analytics.domain.RequestContext;
import com.example.analytics.repository.AnalyticsRepository;
import com.example.analytics.service.config.MetricDefinitionProvider;
import com.example.analytics.service.query.BigQueryQueryBuilder;
import com.example.analytics.service.query.BuiltQuery;
import com.example.analytics.service.validation.RequestValidator;
import com.example.analytics.web.ApiException;
import com.example.analytics.web.dto.AnalyticsPageResponse;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import com.example.analytics.web.dto.AnalyticsQueryResponse;
import com.example.analytics.web.dto.AnalyticsRowResponse;
import com.example.analytics.web.dto.ResponseMetadata;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsQueryService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsQueryService.class);

    private final RequestValidator requestValidator;
    private final MetricDefinitionProvider metricDefinitionProvider;
    private final BigQueryQueryBuilder bigQueryQueryBuilder;
    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsProperties analyticsProperties;
    private final ResultPageCache resultPageCache;
    private final PageTokenService pageTokenService;

    public AnalyticsQueryService(
            RequestValidator requestValidator,
            MetricDefinitionProvider metricDefinitionProvider,
            BigQueryQueryBuilder bigQueryQueryBuilder,
            AnalyticsRepository analyticsRepository,
            AnalyticsProperties analyticsProperties,
            ResultPageCache resultPageCache,
            PageTokenService pageTokenService
    ) {
        this.requestValidator = requestValidator;
        this.metricDefinitionProvider = metricDefinitionProvider;
        this.bigQueryQueryBuilder = bigQueryQueryBuilder;
        this.analyticsRepository = analyticsRepository;
        this.analyticsProperties = analyticsProperties;
        this.resultPageCache = resultPageCache;
        this.pageTokenService = pageTokenService;
    }

    public AnalyticsQueryResponse executeQuery(AnalyticsQueryRequest request, RequestContext context) {
        log.info("Processing analytics query metricName={} tenantId={} memberId={}",
                request.getMetricName(), context.tenantId(), context.memberId());
        requestValidator.validate(request);
        MetricDefinition metricDefinition = metricDefinitionProvider.getRequiredMetric(request.getMetricName());
        BuiltQuery builtQuery = bigQueryQueryBuilder.build(request, context, metricDefinition);
        QueryExecutionResult queryExecutionResult = analyticsRepository.execute(builtQuery);

        List<AnalyticsRow> rows = queryExecutionResult.rows();
        int pageSize = analyticsProperties.getDefaultPageSize();
        List<AnalyticsRow> firstPageRows = rows.size() > pageSize ? rows.subList(0, pageSize) : rows;

        String nextPageToken = null;
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            nextPageToken = cacheOverflowPages(rows, pageSize, context);
        }

        return new AnalyticsQueryResponse(
                request.getMetricName(),
                context.tenantId(),
                context.memberId(),
                request.getDateStart(),
                request.getDateEnd(),
                toRowResponses(firstPageRows),
                new ResponseMetadata(
                        firstPageRows.size(),
                        hasMore,
                        nextPageToken,
                        queryExecutionResult.executionTimeMs(),
                        queryExecutionResult.bigQueryJobId()
                )
        );
    }

    public AnalyticsPageResponse getNextPage(String pageToken, RequestContext context) {
        CachedPage cachedPage = resultPageCache.get(pageToken);
        if (cachedPage == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PAGE_TOKEN_NOT_FOUND", "The requested page token does not exist or expired");
        }
        if (!cachedPage.tenantId().equals(context.tenantId()) || !cachedPage.memberId().equals(context.memberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PAGE_TOKEN_OWNERSHIP_MISMATCH",
                    "The requested page token does not belong to the provided tenant/member context");
        }

        return new AnalyticsPageResponse(
                toRowResponses(cachedPage.rows()),
                new ResponseMetadata(cachedPage.rows().size(), cachedPage.hasMore(), cachedPage.nextPageToken(), null, null)
        );
    }

    private String cacheOverflowPages(List<AnalyticsRow> rows, int pageSize, RequestContext context) {
        String resultSetId = pageTokenService.generateResultSetId();
        List<List<AnalyticsRow>> chunks = new ArrayList<>();
        for (int start = pageSize; start < rows.size(); start += pageSize) {
            int end = Math.min(start + pageSize, rows.size());
            chunks.add(rows.subList(start, end));
        }

        String initialToken = null;
        String nextToken = null;
        for (int index = chunks.size() - 1; index >= 0; index--) {
            String currentToken = pageTokenService.generatePageToken();
            CachedPage page = new CachedPage(
                    resultSetId,
                    List.copyOf(chunks.get(index)),
                    context.tenantId(),
                    context.memberId(),
                    nextToken != null,
                    nextToken
            );
            resultPageCache.put(currentToken, page);
            nextToken = currentToken;
            initialToken = currentToken;
        }
        return initialToken;
    }

    private List<AnalyticsRowResponse> toRowResponses(List<AnalyticsRow> rows) {
        return rows.stream()
                .map(row -> new AnalyticsRowResponse(row.date(), row.value()))
                .toList();
    }
}
