package com.example.analytics.service.config;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.domain.MetricDefinition;
import com.example.analytics.domain.MetricDefinitionsDocument;
import com.example.analytics.web.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReloadingMetricDefinitionProvider implements MetricDefinitionProvider {

    private static final Logger log = LoggerFactory.getLogger(ReloadingMetricDefinitionProvider.class);

    private final AnalyticsProperties analyticsProperties;
    private final ResourceLoader resourceLoader;
    private final MetricConfigValidator metricConfigValidator;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AtomicReference<Map<String, MetricDefinition>> metricSnapshot = new AtomicReference<>(Map.of());

    public ReloadingMetricDefinitionProvider(
            AnalyticsProperties analyticsProperties,
            ResourceLoader resourceLoader,
            MetricConfigValidator metricConfigValidator
    ) {
        this.analyticsProperties = analyticsProperties;
        this.resourceLoader = resourceLoader;
        this.metricConfigValidator = metricConfigValidator;
    }

    @PostConstruct
    public void loadOnStartup() {
        reloadMetrics(true);
    }

    @Scheduled(fixedDelayString = "#{@analyticsProperties.metricReloadInterval.toMillis()}")
    public void scheduledReload() {
        reloadMetrics(false);
    }

    @Override
    public MetricDefinition getRequiredMetric(String metricName) {
        MetricDefinition metricDefinition = metricSnapshot.get().get(metricName);
        if (metricDefinition == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_METRIC", "Unsupported metric_name: " + metricName);
        }
        return metricDefinition;
    }

    private void reloadMetrics(boolean failFast) {
        try {
            MetricDefinitionsDocument document = readDocument();
            metricConfigValidator.validate(document);
            Map<String, MetricDefinition> metrics = document.getMetrics().stream()
                    .collect(Collectors.toUnmodifiableMap(MetricDefinition::getName, Function.identity()));
            metricSnapshot.set(metrics);
            log.info("Metric configuration reloaded successfully with metricCount={}", metrics.size());
        } catch (Exception exception) {
            log.error("Metric configuration reload failed", exception);
            if (failFast) {
                if (exception instanceof ApiException apiException) {
                    throw apiException;
                }
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_METRIC_CONFIGURATION",
                        "Failed to load metric configuration");
            }
        }
    }

    private MetricDefinitionsDocument readDocument() throws IOException {
        String location = analyticsProperties.getMetricsConfigLocation();
        Resource resource = resourceLoader.getResource(location.startsWith("classpath:") ? location : "classpath:" + location);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, MetricDefinitionsDocument.class);
        }
    }
}
