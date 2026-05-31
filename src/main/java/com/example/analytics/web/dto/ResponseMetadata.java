package com.example.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseMetadata(
        @JsonProperty("row_count") int rowCount,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_page_token") String nextPageToken,
        @JsonProperty("execution_time_ms") Long executionTimeMs,
        @JsonProperty("big_query_job_id") String bigQueryJobId
) {
}
