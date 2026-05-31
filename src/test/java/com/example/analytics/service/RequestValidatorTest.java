package com.example.analytics.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.analytics.config.AnalyticsProperties;
import com.example.analytics.service.validation.RequestValidator;
import com.example.analytics.web.ApiException;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestValidatorTest {

    private RequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setMaxDateRangeDays(31);
        requestValidator = new RequestValidator(properties);
    }

    @Test
    void shouldAcceptValidRequest() {
        AnalyticsQueryRequest request = request("daily_active_users", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        assertDoesNotThrow(() -> requestValidator.validate(request));
    }

    @Test
    void shouldRejectStartDateAfterEndDate() {
        AnalyticsQueryRequest request = request("daily_active_users", LocalDate.now().minusDays(1), LocalDate.now().minusDays(2));

        ApiException exception = assertThrows(ApiException.class, () -> requestValidator.validate(request));

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
    }

    @Test
    void shouldRejectFutureDates() {
        AnalyticsQueryRequest request = request("daily_active_users", LocalDate.now(), LocalDate.now().plusDays(1));

        ApiException exception = assertThrows(ApiException.class, () -> requestValidator.validate(request));

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
    }

    @Test
    void shouldRejectTooLargeDateRange() {
        AnalyticsQueryRequest request = request("daily_active_users", LocalDate.now().minusDays(40), LocalDate.now().minusDays(1));

        ApiException exception = assertThrows(ApiException.class, () -> requestValidator.validate(request));

        assertEquals("DATE_RANGE_TOO_LARGE", exception.getErrorCode());
    }

    private AnalyticsQueryRequest request(String metricName, LocalDate dateStart, LocalDate dateEnd) {
        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setMetricName(metricName);
        request.setDateStart(dateStart);
        request.setDateEnd(dateEnd);
        return request;
    }
}
