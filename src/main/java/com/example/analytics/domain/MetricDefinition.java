package com.example.analytics.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricDefinition {

    private String name;
    private String table;
    @JsonProperty("date_column")
    private String dateColumn;
    @JsonProperty("value_column")
    private String valueColumn;
    @JsonProperty("tenant_column")
    private String tenantColumn;
    @JsonProperty("member_column")
    private String memberColumn;
    @JsonProperty("aggregation_type")
    private AggregationType aggregationType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getDateColumn() {
        return dateColumn;
    }

    public void setDateColumn(String dateColumn) {
        this.dateColumn = dateColumn;
    }

    public String getValueColumn() {
        return valueColumn;
    }

    public void setValueColumn(String valueColumn) {
        this.valueColumn = valueColumn;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public String getMemberColumn() {
        return memberColumn;
    }

    public void setMemberColumn(String memberColumn) {
        this.memberColumn = memberColumn;
    }

    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }
}
