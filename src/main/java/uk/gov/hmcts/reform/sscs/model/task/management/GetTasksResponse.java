package uk.gov.hmcts.reform.sscs.model.task.management;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Builder
public class GetTasksResponse {

    private List<Task> tasks;
    private long totalRecords;

}