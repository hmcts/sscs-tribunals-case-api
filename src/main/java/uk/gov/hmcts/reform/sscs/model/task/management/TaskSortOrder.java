package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TaskSortOrder {

    ASCENDANT("asc"),
    DESCENDANT("desc");

    @JsonValue
    private final String id;

    @Override
    public String toString() {
        return id;
    }
}
