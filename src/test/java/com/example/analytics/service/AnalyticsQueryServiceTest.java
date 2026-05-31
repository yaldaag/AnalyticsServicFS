package com.example.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.google.cloud.bigquery.QueryJobConfiguration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AnalyticsQueryServiceTest {

    private final RequestValidator requestValidator = mock(RequestValidator.class);
    private final MetricDefinitionProvider metricDefinitionProvider = mock(MetricDefinitionProvider.class);
    private final BigQueryQueryBuilder bigQueryQueryBuilder = mock(BigQueryQueryBuilder.class);
    private final AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);
    private final ResultPageCache resultPageCache = mock(ResultPageCache.class);
    private final PageTokenService pageTokenService = mock(PageTokenService.class);

    private AnalyticsQueryService analyticsQueryService;

    @BeforeEach
    void setUp() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDefaultPageSize(2);
        analyticsQueryService = new AnalyticsQueryService(
                requestValidator,
                metricDefinitionProvider,
                bigQueryQueryBuilder,
                analyticsRepository,
                properties,
                resultPageCache,
                pageTokenService
        );
    }

    @Test
    void shouldReturnSinglePageWithoutCaching() {
        AnalyticsQueryRequest request = request();
        RequestContext context = new RequestContext("wix", "member-1");

        when(metricDefinitionProvider.getRequiredMetric("daily_active_users")).thenReturn(new MetricDefinition());
        when(bigQueryQueryBuilder.build(any(), any(), any())).thenReturn(new BuiltQuery("select 1", QueryJobConfiguration.newBuilder("select 1").build()));
        when(analyticsRepository.execute(any())).thenReturn(new QueryExecutionResult(
                List.of(
                        new AnalyticsRow(LocalDate.parse("2024-01-01"), 10),
                        new AnalyticsRow(LocalDate.parse("2024-01-02"), 11)
                ),
                12,
                "job-1"
        ));
        doNothing().when(requestValidator).validate(request);

        AnalyticsQueryResponse response = analyticsQueryService.executeQuery(request, context);

        assertEquals(2, response.rows().size());
        assertFalse(response.metadata().hasMore());
        assertNull(response.metadata().nextPageToken());
    }

    @Test
    void shouldCacheOverflowPagesAndReturnFirstNextToken() {
        AnalyticsQueryRequest request = request();
        RequestContext context = new RequestContext("wix", "member-1");

        when(metricDefinitionProvider.getRequiredMetric("daily_active_users")).thenReturn(new MetricDefinition());
        when(bigQueryQueryBuilder.build(any(), any(), any())).thenReturn(new BuiltQuery("select 1", QueryJobConfiguration.newBuilder("select 1").build()));
        when(analyticsRepository.execute(any())).thenReturn(new QueryExecutionResult(
                List.of(
                        new AnalyticsRow(LocalDate.parse("2024-01-01"), 10),
                        new AnalyticsRow(LocalDate.parse("2024-01-02"), 11),
                        new AnalyticsRow(LocalDate.parse("2024-01-03"), 12),
                        new AnalyticsRow(LocalDate.parse("2024-01-04"), 13),
                        new AnalyticsRow(LocalDate.parse("2024-01-05"), 14)
                ),
                20,
                "job-2"
        ));
        when(pageTokenService.generateResultSetId()).thenReturn("result-set-1");
        when(pageTokenService.generatePageToken()).thenReturn("token-last", "token-first");

        Map<String, CachedPage> cache = new ConcurrentHashMap<>();
        Mockito.doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(resultPageCache).put(any(), any());

        AnalyticsQueryResponse response = analyticsQueryService.executeQuery(request, context);

        assertEquals(2, response.rows().size());
        assertTrue(response.metadata().hasMore());
        assertEquals("token-first", response.metadata().nextPageToken());
        assertEquals(2, cache.get("token-first").rows().size());
        assertTrue(cache.get("token-first").hasMore());
        assertEquals("token-last", cache.get("token-first").nextPageToken());
        assertEquals(1, cache.get("token-last").rows().size());
        assertFalse(cache.get("token-last").hasMore());
    }

    @Test
    void shouldRejectMismatchedPageOwnership() {
        when(resultPageCache.get("token-1")).thenReturn(new CachedPage(
                "result-set-1",
                List.of(new AnalyticsRow(LocalDate.parse("2024-01-01"), 10)),
                "tenant-a",
                "member-a",
                false,
                null
        ));

        ApiException exception = assertThrows(ApiException.class,
                () -> analyticsQueryService.getNextPage("token-1", new RequestContext("tenant-b", "member-a")));

        assertEquals("PAGE_TOKEN_OWNERSHIP_MISMATCH", exception.getErrorCode());
    }

    @Test
    void shouldReturnCachedPage() {
        when(resultPageCache.get("token-1")).thenReturn(new CachedPage(
                "result-set-1",
                List.of(new AnalyticsRow(LocalDate.parse("2024-01-02"), 10)),
                "wix",
                "member-1",
                false,
                null
        ));

        AnalyticsPageResponse response = analyticsQueryService.getNextPage("token-1", new RequestContext("wix", "member-1"));

        assertEquals(1, response.rows().size());
        assertFalse(response.metadata().hasMore());
        assertNull(response.metadata().nextPageToken());
    }

    private AnalyticsQueryRequest request() {
        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setMetricName("daily_active_users");
        request.setDateStart(LocalDate.parse("2024-01-01"));
        request.setDateEnd(LocalDate.parse("2024-01-31"));
        return request;
    }
}
