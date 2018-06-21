package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseData {
    private String caseReference;
    private String caseCreated;
    private Appeal appeal;
    private List<Hearing> hearings;
    private Evidence evidence;
    private List<DwpTimeExtension> dwpTimeExtension;
    private List<Event> events;
    private Subscriptions subscriptions;
    private RegionalProcessingCenter regionalProcessingCenter;
    private List<SscsDocument> sscsDocument;

    @JsonCreator
    public CaseData(@JsonProperty("caseReference") String caseReference,
                    @JsonProperty("caseCreated") String caseCreated,
                    @JsonProperty("appeal") Appeal appeal,
                    @JsonProperty("hearings") List<Hearing> hearings,
                    @JsonProperty("evidence") Evidence evidence,
                    @JsonProperty("dwpTimeExtension") List<DwpTimeExtension> dwpTimeExtension,
                    @JsonProperty("events") List<Event> events,
                    @JsonProperty("subscriptions") Subscriptions subscriptions,
                    @JsonProperty("regionalProcessingCenter")  RegionalProcessingCenter regionalProcessingCenter,
                    @JsonProperty("sscsDocument") List<SscsDocument> sscsDocument) {
        this.caseReference = caseReference;
        this.caseCreated = caseCreated;
        this.appeal = appeal;
        this.hearings = hearings;
        this.evidence = evidence;
        this.dwpTimeExtension = dwpTimeExtension;
        this.events = events;
        this.subscriptions = subscriptions;
        this.regionalProcessingCenter = regionalProcessingCenter;
        this.sscsDocument = sscsDocument;
    }
}
