package uk.gov.hmcts.reform.sscs.model.service.hearingvalues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.model.HearingLocation;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HearingType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseCategory;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingWindow;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@SuppressWarnings("PMD.TooManyFields")
@Data
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceHearingValues {

    private String caseDeepLink;

    private String caseManagementLocationCode;

    @JsonProperty("caserestrictedFlag")
    private boolean caseRestrictedFlag;

    @JsonProperty("caseSLAStartDate")
    private String caseSlaStartDate;

    private String externalCaseReference;

    private List<HearingChannel> hearingChannels;

    private String hmctsInternalCaseName;

    private String publicCaseName;

    private boolean autoListFlag;

    private HearingType hearingType;

    private String caseType;

    private List<CaseCategory> caseCategories;

    @JsonProperty("hearingWindow")
    private HearingWindow hearingWindow;

    private Integer duration;

    private String hearingPriorityType;

    private Integer numberOfPhysicalAttendees;

    private boolean hearingInWelshFlag;

    @JsonProperty("hearingLocations")
    private List<HearingLocation> hearingLocations;

    private Boolean caseAdditionalSecurityFlag;

    private List<String> facilitiesRequired;

    private String listingComments;

    private String hearingRequester;

    private boolean privateHearingRequiredFlag;

    @JsonProperty("panelRequirements")
    private PanelRequirements panelRequirements;

    private String leadJudgeContractType;

    @JsonProperty("judiciary")
    private Judiciary judiciary;

    private boolean hearingIsLinkedFlag;

    @JsonProperty("parties")
    private List<PartyDetails> parties;

    @JsonProperty("caseFlags")
    private CaseFlags caseFlags;

    @JsonProperty("screenFlow")
    private List<ScreenNavigation> screenFlow;

    @JsonProperty("vocabulary")
    private List<Vocabulary> vocabulary;

    private String hmctsServiceID;

    private boolean caseInterpreterRequiredFlag;
}
