package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class AdjournCaseMidEventValidationHandlerTest extends AdjournCaseMidEventValidationHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot specify both directions due date and directions due days offset");
    }

    @Test
    void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecifiedButZero_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.OTHER);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenDirectionsDueDateIsNotSpecifiedAndDaysOffsetSpecified_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNeitherDirectionsDueDateOrOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("At least one of directions due date or directions due date offset must be specified");
    }

    @Test
    void givenNeitherDirectionsDueDateOrOffsetSpecifiedButNoDirectionsToParties_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().plusDays(1));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsToday_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDisplayAnError() {

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().minusDays(1));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("'First available date after' date cannot be in the past");
    }

    @Test
    void givenNextHearingDateNotRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDoNotDisplayAnError() {

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().minusDays(1));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingDateNotSet_ThenDisplayAnError() {

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("'First available date after' date must be provided");
    }

    @Test
    void givenInvalidListingDurationThrowError() {
        sscsCaseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(32);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors())
                .hasSize(1)
                .containsOnly("Duration length needs to be a multiple of 5");

    }

    @Test
    void givenValidListingDurationDoNotThrowError() {
        sscsCaseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(30);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenInvalidListingSessionsThrowError() {
        sscsCaseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(12);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors())
                .hasSize(1)
                .containsOnly("Duration length cannot be greater than 8");

    }

    @Test
    void givenValidListingSessionsDoNotThrowError() {
        sscsCaseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(5);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

}
