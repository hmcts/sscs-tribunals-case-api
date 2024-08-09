package uk.gov.hmcts.reform.sscs.model.multi.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListAssistCaseStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseHearing {
    private List<HearingChannel> hearingChannels;
    private List<HearingDaySchedule> hearingDaySchedule;
    private String hearingGroupRequestId;
    @JsonProperty("hearingID")
    private Long hearingId;
    private Boolean hearingIsLinkedFlag;
    private ListingStatus hearingListingStatus;
    private LocalDateTime hearingRequestDateTime;
    private String hearingType;
    private HmcStatus hmcStatus;
    private LocalDateTime lastResponseReceivedDateTime;
    private ListAssistCaseStatus listAssistCaseStatus;
    private Long requestVersion;
}
