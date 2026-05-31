package com.example.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsQueryResponse(
        @JsonProperty("metric_name") String metricName,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("member_id") String memberId,
        @JsonProperty("date_start") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateStart,
        @JsonProperty("date_end") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateEnd,
        List<AnalyticsRowResponse> rows,
        ResponseMetadata metadata
) {
}
