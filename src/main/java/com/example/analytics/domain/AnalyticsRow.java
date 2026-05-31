package com.example.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;

public record AnalyticsRow(LocalDate date, long value) implements Serializable {
}
