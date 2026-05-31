package com.example.analytics.controller;

import com.example.analytics.domain.RequestContext;
import com.example.analytics.service.AnalyticsQueryService;
import com.example.analytics.web.dto.AnalyticsPageResponse;
import com.example.analytics.web.dto.AnalyticsQueryRequest;
import com.example.analytics.web.dto.AnalyticsQueryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/analytics/query")
public class AnalyticsQueryController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsQueryController.class);

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsQueryController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @PostMapping
    public AnalyticsQueryResponse executeQuery(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @RequestHeader("X-Member-Id") @NotBlank String memberId,
            @Valid @RequestBody AnalyticsQueryRequest request
    ) {
        log.info("Received analytics query request");
        return analyticsQueryService.executeQuery(request, new RequestContext(tenantId, memberId));
    }

    @GetMapping("/pages/{pageToken}")
    public AnalyticsPageResponse getPage(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @RequestHeader("X-Member-Id") @NotBlank String memberId,
            @PathVariable("pageToken") String pageToken
    ) {
        log.info("Received analytics page request");
        return analyticsQueryService.getNextPage(pageToken, new RequestContext(tenantId, memberId));
    }
}
