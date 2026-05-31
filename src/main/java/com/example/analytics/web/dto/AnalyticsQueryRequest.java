package com.example.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AnalyticsQueryRequest {

    @NotBlank
    @JsonProperty("metric_name")
    private String metricName;

    @NotNull
    @JsonProperty("date_start")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateStart;

    @NotNull
    @JsonProperty("date_end")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateEnd;

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public LocalDate getDateStart() {
        return dateStart;
    }

    public void setDateStart(LocalDate dateStart) {
        this.dateStart = dateStart;
    }

    public LocalDate getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDate dateEnd) {
        this.dateEnd = dateEnd;
    }
}
