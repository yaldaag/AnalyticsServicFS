package com.example.analytics.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analytics")
public class AnalyticsProperties {

    @Min(1)
    private int defaultPageSize = 100;

    @Min(1)
    private int maxDateRangeDays = 366;

    private Duration pageTtl = Duration.ofMinutes(15);

    private Duration metricReloadInterval = Duration.ofHours(1);

    private Duration bigQueryTimeout = Duration.ofSeconds(30);

    private Long maxBytesBilled;

    @NotBlank
    private String metricsConfigLocation = "metrics/metrics.yml";

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxDateRangeDays() {
        return maxDateRangeDays;
    }

    public void setMaxDateRangeDays(int maxDateRangeDays) {
        this.maxDateRangeDays = maxDateRangeDays;
    }

    public Duration getPageTtl() {
        return pageTtl;
    }

    public void setPageTtl(Duration pageTtl) {
        this.pageTtl = pageTtl;
    }

    public Duration getMetricReloadInterval() {
        return metricReloadInterval;
    }

    public void setMetricReloadInterval(Duration metricReloadInterval) {
        this.metricReloadInterval = metricReloadInterval;
    }

    public Duration getBigQueryTimeout() {
        return bigQueryTimeout;
    }

    public void setBigQueryTimeout(Duration bigQueryTimeout) {
        this.bigQueryTimeout = bigQueryTimeout;
    }

    public Long getMaxBytesBilled() {
        return maxBytesBilled;
    }

    public void setMaxBytesBilled(Long maxBytesBilled) {
        this.maxBytesBilled = maxBytesBilled;
    }

    public String getMetricsConfigLocation() {
        return metricsConfigLocation;
    }

    public void setMetricsConfigLocation(String metricsConfigLocation) {
        this.metricsConfigLocation = metricsConfigLocation;
    }
}
