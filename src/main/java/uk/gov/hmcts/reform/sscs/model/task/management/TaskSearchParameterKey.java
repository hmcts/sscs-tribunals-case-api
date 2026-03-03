package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TaskSearchParameterKey {

    USER("user"),
    JURISDICTION("jurisdiction"),
    CASE_ID("case_id");

    @JsonValue
    private final String id;

    @Override
    public String toString() {
        return id;
    }

    public String value() {
        return id;
    }
}
