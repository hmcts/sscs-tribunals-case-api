package uk.gov.hmcts.reform.sscs.model.task.management;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("task_title")
    private String taskTitle;
    @JsonProperty("created_date")
    private ZonedDateTime createdDate;
    @JsonProperty("due_date")
    private ZonedDateTime dueDate;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("additional_properties")
    private Map<String, String> additionalProperties;
}