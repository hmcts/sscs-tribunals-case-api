package uk.gov.hmcts.reform.sscs.model.task.management;

import static java.util.Arrays.stream;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TaskSearchOperator {

    IN("IN"),
    CONTEXT("CONTEXT"),
    BOOLEAN("BOOLEAN"),
    BETWEEN("BETWEEN"),
    BEFORE("BEFORE"),
    AFTER("AFTER");

    @JsonValue
    private String value;

    public static TaskSearchOperator from(String value) {
        return stream(values())
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(value + " is an unsupported operator"));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
