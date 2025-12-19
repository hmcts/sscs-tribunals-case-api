package uk.gov.hmcts.reform.sscs.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
public class CamundaTask {
    private final String id;
    private final String name;
    private final String processInstanceId;

    public CamundaTask(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("processInstanceId") String processInstanceId) {
        this.id = id;
        this.name = name;
        this.processInstanceId = processInstanceId;
    }
}
