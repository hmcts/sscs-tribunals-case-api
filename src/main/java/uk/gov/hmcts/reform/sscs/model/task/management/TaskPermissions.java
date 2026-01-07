package uk.gov.hmcts.reform.sscs.model.task.management;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class TaskPermissions {

    Set<TaskPermissionTypes> values;

    public Set<TaskPermissionTypes> getValues() {
        return values;
    }
}
