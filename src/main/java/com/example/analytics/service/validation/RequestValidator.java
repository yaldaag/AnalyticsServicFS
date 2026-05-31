package com.example.analytics.service.validation;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.web.ApiException;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

    private final AnalyticsProperties analyticsProperties;

    public RequestValidator(AnalyticsProperties analyticsProperties) {
        this.analyticsProperties = analyticsProperties;
    }

    public void validate(AnalyticsQueryRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getDateStart().isAfter(request.getDateEnd())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "date_start must be before or equal to date_end");
        }
        if (request.getDateStart().isAfter(today) || request.getDateEnd().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "dates in the future are not allowed");
        }

        long dateRangeDays = ChronoUnit.DAYS.between(request.getDateStart(), request.getDateEnd()) + 1;
        if (dateRangeDays > analyticsProperties.getMaxDateRangeDays()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LARGE",
                    "Requested date range exceeds the configured maximum");
        }
    }
}
