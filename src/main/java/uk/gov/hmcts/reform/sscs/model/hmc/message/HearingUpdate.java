package uk.gov.hmcts.reform.sscs.model.hmc.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListAssistCaseStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingUpdate {
    private LocalDateTime hearingResponseReceivedDateTime;
    private LocalDateTime hearingEventBroadcastDateTime;
    @NonNull
    @JsonProperty("HMCStatus")
    private HmcStatus hmcStatus;
    @JsonProperty("hearingListingStatus")
    private ListingStatus listingStatus;
    private LocalDateTime nextHearingDate;
    @JsonProperty("ListAssistCaseStatus")
    private ListAssistCaseStatus listAssistCaseStatus;
    private String listAssistSessionID;
    @JsonProperty("hearingVenueId")
    private String hearingEpimsId;
    private String hearingRoomId;
    private String hearingJudgeId;
}
