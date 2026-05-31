package com.example.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.analytics.service.AnalyticsQueryService;
import com.example.analytics.web.ApiException;
import com.example.analytics.web.GlobalExceptionHandler;
import com.example.analytics.web.dto.AnalyticsPageResponse;
import com.example.analytics.web.dto.AnalyticsQueryResponse;
import com.example.analytics.web.dto.AnalyticsRowResponse;
import com.example.analytics.web.dto.ResponseMetadata;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AnalyticsQueryController.class)
@Import(GlobalExceptionHandler.class)
class AnalyticsQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsQueryService analyticsQueryService;

    @Test
    void shouldReturnSuccessfulQueryResponse() throws Exception {
        AnalyticsQueryResponse response = new AnalyticsQueryResponse(
                "daily_active_users",
                "wix",
                "member-1",
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-02"),
                List.of(new AnalyticsRowResponse(LocalDate.parse("2024-01-01"), 10)),
                new ResponseMetadata(1, false, null, 25L, "job-1")
        );

        when(analyticsQueryService.executeQuery(any(), any())).thenReturn(response);

        mockMvc.perform(post("/analytics/query")
                        .header("X-Tenant-Id", "wix")
                        .header("X-Member-Id", "member-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "metric_name": "daily_active_users",
                                  "date_start": "2024-01-01",
                                  "date_end": "2024-01-02"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric_name").value("daily_active_users"))
                .andExpect(jsonPath("$.tenant_id").value("wix"))
                .andExpect(jsonPath("$.metadata.has_more").value(false));
    }

    @Test
    void shouldReturnBadRequestForMissingHeader() throws Exception {
        mockMvc.perform(post("/analytics/query")
                        .header("X-Member-Id", "member-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "metric_name": "daily_active_users",
                                  "date_start": "2024-01-01",
                                  "date_end": "2024-01-02"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMalformedDates() throws Exception {
        mockMvc.perform(post("/analytics/query")
                        .header("X-Tenant-Id", "wix")
                        .header("X-Member-Id", "member-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "metric_name": "daily_active_users",
                                  "date_start": "bad-date",
                                  "date_end": "2024-01-02"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturnConfiguredApiError() throws Exception {
        when(analyticsQueryService.getNextPage(eq("missing-token"), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "PAGE_TOKEN_NOT_FOUND",
                        "The requested page token does not exist or expired"));

        mockMvc.perform(get("/analytics/query/pages/missing-token")
                        .header("X-Tenant-Id", "wix")
                        .header("X-Member-Id", "member-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PAGE_TOKEN_NOT_FOUND"));
    }
}
