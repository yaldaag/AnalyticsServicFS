package com.example.analytics.web.dto;

import java.util.List;

public record AnalyticsPageResponse(
        List<AnalyticsRowResponse> rows,
        ResponseMetadata metadata
) {
}
