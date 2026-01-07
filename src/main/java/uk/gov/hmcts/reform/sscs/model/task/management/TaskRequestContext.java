package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@JsonFormat(shape = JsonFormat.Shape.STRING)
@AllArgsConstructor
public enum TaskRequestContext {

    ALL_WORK("ALL_WORK"),
    AVAILABLE_TASKS("AVAILABLE_TASKS");

    @JsonValue
    private final String id;

    @Override
    public String toString() {
        return id;
    }
}