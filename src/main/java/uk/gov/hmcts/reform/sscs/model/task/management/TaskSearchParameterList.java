package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
public class TaskSearchParameterList implements TaskSearchParameter<List<String>> {

    private TaskSearchParameterKey key;
    private TaskSearchOperator operator;

    @JsonProperty("values")
    private List<String> values;
}
