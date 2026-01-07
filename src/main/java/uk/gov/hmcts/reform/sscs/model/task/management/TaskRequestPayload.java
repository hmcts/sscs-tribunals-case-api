package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class TaskRequestPayload {

    @JsonProperty("search_parameters")
    private List<TaskSearchParameter<?>> searchParameters;

    @JsonProperty("sorting_parameters")
    private List<TaskSortingParameter> sortingParameters;

    @JsonProperty("request_context")
    private TaskRequestContext requestContext;
}