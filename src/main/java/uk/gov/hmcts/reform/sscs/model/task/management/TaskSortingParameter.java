package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Builder
public class TaskSortingParameter {

    @JsonProperty("sort_by")
    private TaskSortField sortBy;

    @JsonProperty("sort_order")
    private TaskSortOrder sortOrder;
}
