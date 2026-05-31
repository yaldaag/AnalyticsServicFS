package com.example.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AnalyticsRowResponse(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        long value
) {
}
