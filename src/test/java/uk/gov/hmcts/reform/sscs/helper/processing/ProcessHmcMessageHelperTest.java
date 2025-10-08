package uk.gov.hmcts.reform.sscs.helper.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;

@ExtendWith(MockitoExtension.class)
class ProcessHmcMessageHelperTest {

    @InjectMocks
    ProcessHmcMessageHelper hmcMessageHelper;

    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"LISTED", "AWAITING_LISTING", "UPDATE_SUBMITTED"})
    void stateNotHandledReturnTrueForGivenHmcStatus(HmcStatus hmcStatus) {
        boolean result = hmcMessageHelper.stateNotHandled(hmcStatus, getHearingResponse());
        assertThat(result).isTrue();
    }

    @Test
    void stateNotHandledReturnFalseForGivenHmcStatus() {
        boolean result = hmcMessageHelper.stateNotHandled(HmcStatus.CANCELLED, getHearingResponse());
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"LISTED", "AWAITING_LISTING", "UPDATE_SUBMITTED"})
    void isHearingUpdatedReturnTrueForGivenHmcStatus(HmcStatus hmcStatus) {
        HearingGetResponse hearingGetResponse = getHearingResponse();
        hearingGetResponse.getHearingResponse().setListingStatus(ListingStatus.FIXED);
        boolean result = hmcMessageHelper.isHearingUpdated(hmcStatus, hearingGetResponse);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"HEARING_REQUESTED", "UPDATE_REQUESTED", "CANCELLATION_REQUESTED",
        "CANCELLATION_SUBMITTED", "CANCELLED", "AWAITING_ACTUALS", "COMPLETED", "ADJOURNED", "NOT_FOUND"})
    void isHearingUpdatedReturnFalseForGivenHmcStatus(HmcStatus hmcStatus) {
        HearingGetResponse hearingGetResponse = getHearingResponse();
        hearingGetResponse.getHearingResponse().setListingStatus(ListingStatus.FIXED);
        boolean result = hmcMessageHelper.isHearingUpdated(hmcStatus, hearingGetResponse);
        assertThat(result).isFalse();
    }

    @Test
    void isStatusExceptionWhenStatusIsException() {
        boolean result = hmcMessageHelper.isStatusException(HmcStatus.EXCEPTION);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"HEARING_REQUESTED", "AWAITING_LISTING", "LISTED", "UPDATE_REQUESTED",
        "UPDATE_SUBMITTED", "CANCELLATION_REQUESTED", "CANCELLATION_SUBMITTED", "CANCELLED", "AWAITING_ACTUALS", "COMPLETED",
        "ADJOURNED", "NOT_FOUND"})
    void isStatusExceptionWhenStatusIsNotException(HmcStatus hmcStatus) {
        boolean result = hmcMessageHelper.isStatusException(hmcStatus);
        assertThat(result).isFalse();
    }

    private HearingGetResponse getHearingResponse() {
        return HearingGetResponse.builder()
            .requestDetails(RequestDetails.builder().build())
            .hearingDetails(HearingDetails.builder().build())
            .caseDetails(CaseDetails.builder().build())
            .partyDetails(new ArrayList<>())
            .hearingResponse(HearingResponse.builder().build())
            .build();
    }
}
