package uk.gov.hmcts.reform.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@ToString
@EqualsAndHashCode
@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaTask {

    private String id;
    private String name;
    private String assignee;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private ZonedDateTime created;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private ZonedDateTime due;
    private String description;
    private String owner;
    private String formKey;
    private String processInstanceId;

    private CamundaTask() {
        //Hidden constructor
        super();
    }

    public CamundaTask(String id,
                       String name,
                       String assignee,
                       ZonedDateTime created,
                       ZonedDateTime due,
                       String description,
                       String owner,
                       String formKey,
                       String processInstanceId
    ) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.created = created;
        this.due = due;
        this.description = description;
        this.owner = owner;
        this.formKey = formKey;
        this.processInstanceId = processInstanceId;
    }

}