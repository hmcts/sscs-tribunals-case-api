package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlineHearing {
    private final String onlineHearingId;
    private final String appellantName;
    private final String caseReference;
    private final Long caseId;
    private final Decision decision;
    private final FinalDecision finalDecision;
    private final boolean hasFinalDecision;
    private final HearingArrangements hearingArrangements;
    private final AppellantDetails appellantDetails;
    private final AppealDetails appealDetails;

    public OnlineHearing(String onlineHearingId, String appellantName, String caseReference, Long caseId, Decision decision, FinalDecision finalDecision, boolean hasFinalDecision, AppellantDetails appellantDetails, AppealDetails appealDetails) {
        this(onlineHearingId, appellantName, caseReference, caseId, decision, finalDecision, hasFinalDecision, null, appellantDetails, appealDetails);
    }

    public OnlineHearing(String appellantName, String caseReference, Long caseId, HearingArrangements hearingArrangements, AppellantDetails appellantDetails, AppealDetails appealDetails) {
        this(null, appellantName, caseReference, caseId, null, null, false, hearingArrangements, appellantDetails, appealDetails);
    }

    private OnlineHearing(String onlineHearingId, String appellantName, String caseReference, Long caseId, Decision decision, FinalDecision finalDecision, boolean hasFinalDecision, HearingArrangements hearingArrangements, AppellantDetails appellantDetails, AppealDetails appealDetails) {
        this.onlineHearingId = onlineHearingId;
        this.appellantName = appellantName;
        this.caseReference = caseReference;
        this.caseId = caseId;
        this.decision = decision;
        this.finalDecision = finalDecision;
        this.hasFinalDecision = hasFinalDecision;
        this.hearingArrangements = hearingArrangements;
        this.appellantDetails = appellantDetails;
        this.appealDetails = appealDetails;
    }

    @ApiModelProperty(example = "ID_1", required = true)
    @JsonProperty(value = "online_hearing_id")
    public String getOnlineHearingId() {
        return onlineHearingId;
    }

    @ApiModelProperty(example = "Joe Smith", required = true)
    @JsonProperty(value = "appellant_name")
    public String getAppellantName() {
        return appellantName;
    }

    @ApiModelProperty(example = "SC112/233")
    @JsonProperty(value = "case_reference")
    public String getCaseReference() {
        return caseReference;
    }

    @ApiModelProperty(example = "123456789", required = true)
    @JsonProperty(value = "case_id")
    public Long getCaseId() {
        return caseId;
    }

    @JsonProperty(value = "decision")
    public Decision getDecision() {
        return decision;
    }

    @JsonProperty(value = "final_decision")
    public FinalDecision getFinalDecision() {
        return finalDecision;
    }

    @JsonProperty(value = "has_final_decision")
    public boolean isHasFinalDecision() {
        return hasFinalDecision;
    }

    @JsonProperty(value = "hearing_arrangements")
    public HearingArrangements getHearingArrangements() {
        return hearingArrangements;
    }

    @JsonProperty(value = "appellant_details")
    public AppellantDetails getAppellantDetails() {
        return appellantDetails;
    }

    @JsonProperty(value = "appeal_details")
    public AppealDetails getAppealDetails() {
        return appealDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnlineHearing that = (OnlineHearing) o;
        return hasFinalDecision == that.hasFinalDecision
                && Objects.equals(onlineHearingId, that.onlineHearingId)
                && Objects.equals(appellantName, that.appellantName)
                && Objects.equals(caseReference, that.caseReference)
                && Objects.equals(caseId, that.caseId)
                && Objects.equals(decision, that.decision)
                && Objects.equals(finalDecision, that.finalDecision)
                && Objects.equals(hearingArrangements, that.hearingArrangements)
                && Objects.equals(appellantDetails, that.appellantDetails)
                && Objects.equals(appealDetails, that.appealDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onlineHearingId, appellantName, caseReference, caseId, decision, finalDecision, hasFinalDecision, hearingArrangements, appellantDetails, appealDetails);
    }

    @Override
    public String toString() {
        return "OnlineHearing{"
                + "onlineHearingId='" + onlineHearingId + '\''
                + ", appellantName='" + appellantName + '\''
                + ", caseReference='" + caseReference + '\''
                + ", caseId=" + caseId
                + ", decision=" + decision
                + ", finalDecision=" + finalDecision
                + ", hasFinalDecision=" + hasFinalDecision
                + ", hearingArrangements=" + hearingArrangements
                + ", appellantDetails=" + appellantDetails
                + ", appealDetails=" + appealDetails
                + '}';
    }
}
