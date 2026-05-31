package com.example.analytics.domain;

import java.util.ArrayList;
import java.util.List;

public class MetricDefinitionsDocument {

    private List<MetricDefinition> metrics = new ArrayList<>();

    public List<MetricDefinition> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricDefinition> metrics) {
        this.metrics = metrics;
    }
}
