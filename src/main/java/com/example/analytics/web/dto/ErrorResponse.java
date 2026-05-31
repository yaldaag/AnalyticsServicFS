package com.example.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
        @JsonProperty("error_code") String errorCode,
        String message
) {
}
