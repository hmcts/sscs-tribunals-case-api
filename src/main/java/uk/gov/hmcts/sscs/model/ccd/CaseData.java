package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class CaseData {
    private String caseReference;
    private String appealNumber;
    private Appeal appeal;
    private List<Hearing> hearings;
    private Evidence evidence;
    private List<DwpTimeExtension> dwpTimeExtension;
    private List<Event> events;

    @JsonCreator
    public CaseData(@JsonProperty("caseReference") String caseReference,
                    @JsonProperty("appealNumber") String appealNumber,
                    @JsonProperty("appeal") Appeal appeal,
                    @JsonProperty("hearings") List<Hearing> hearings,
                    @JsonProperty("evidence") Evidence evidence,
                    @JsonProperty("dwpTimeExtension") List<DwpTimeExtension> dwpTimeExtension,
                    @JsonProperty("events") List<Event> events) {
        this.caseReference = caseReference;
        this.appealNumber = appealNumber;
        this.appeal = appeal;
        this.hearings = hearings;
        this.evidence = evidence;
        this.dwpTimeExtension = dwpTimeExtension;
        this.events = events;
    }
}
