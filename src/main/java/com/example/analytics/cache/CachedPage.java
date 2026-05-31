package com.example.analytics.cache;

import com.example.analytics.domain.AnalyticsRow;
import java.io.Serializable;
import java.util.List;

public record CachedPage(
        String resultSetId,
        List<AnalyticsRow> rows,
        String tenantId,
        String memberId,
        boolean hasMore,
        String nextPageToken
) implements Serializable {
}
