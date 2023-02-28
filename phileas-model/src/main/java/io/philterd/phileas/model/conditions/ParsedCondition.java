package io.philterd.phileas.model.conditions;

public class ParsedCondition {

    private String field;
    private String operator;
    private String value;

    public ParsedCondition() {

    }

    public ParsedCondition(String field, String operator, String value) {

        this.field = field;
        this.operator = operator;
        this.value = value;

    }

    @Override
    public String toString() {
        return "Field: " + field + "; Operator: " + operator + "; Value: " + value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
