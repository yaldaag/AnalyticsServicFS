package com.example.analytics.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class MetricDefinition {

    private String name;
    private SourceDefinition source;
    private ValueDefinition value;
    private List<PredicateDefinition> predicates = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SourceDefinition getSource() {
        return source;
    }

    public void setSource(SourceDefinition source) {
        this.source = source;
    }

    public ValueDefinition getValue() {
        return value;
    }

    public void setValue(ValueDefinition value) {
        this.value = value;
    }

    public List<PredicateDefinition> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<PredicateDefinition> predicates) {
        this.predicates = predicates != null ? predicates : new ArrayList<>();
    }

    public static class SourceDefinition {

        private String table;
        @JsonProperty("date_column")
        private String dateColumn;
        @JsonProperty("tenant_column")
        private String tenantColumn;
        @JsonProperty("member_column")
        private String memberColumn;

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
    }

    public static class ValueDefinition {

        private AggregationType aggregation;
        private String column;
        private String expression;

        public AggregationType getAggregation() {
            return aggregation;
        }

        public void setAggregation(AggregationType aggregation) {
            this.aggregation = aggregation;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }
    }

    public static class PredicateDefinition {

        private String column;
        private String operator;
        private String literal;

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getLiteral() {
            return literal;
        }

        public void setLiteral(String literal) {
            this.literal = literal;
        }
    }
}
