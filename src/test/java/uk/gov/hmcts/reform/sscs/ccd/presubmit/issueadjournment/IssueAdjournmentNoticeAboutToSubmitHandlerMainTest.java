package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

public class IssueAdjournmentNoticeAboutToSubmitHandlerMainTest extends IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @DisplayName("Given an adjournment event with language interpreter required and case has existing interpreter, "
        + "then overwrite existing interpreter in hearing options")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter(NO.getValue())
            .languages("French")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given an adjournment event with language interpreter required and interpreter language set, "
        + "then do not display error")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndLanguageSet_thenDoNotDisplayError() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("OverrideFields should persist correct epimsid for selected venue in adjournment")
    @Test
    void givenVenueReturnCorrectEpimsInOverrideFields() {
        String epimsId = "epimsId";
        String code = "venueCode";

        when(venueService.getEpimsIdForVenueId(code)).thenReturn(epimsId);

        var item = new DynamicListItem(code, code);
        var list = new DynamicList(item, null);

        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        when(regionalProcessingCenterService.getByVenueId(code)).thenReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();

        var hearingEpimsIds = overrideFields.getHearingVenueEpimsIds();
        assertThat(hearingEpimsIds).isNotNull().hasSize(1).allMatch(b -> b.getValue().getValue().equals(epimsId));
    }

    @DisplayName("Duration is set as existing duration when standard timeslot selected during adjournment")
    @Test
    void givenStandardDurationSelectedShouldSetExistingDuration() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingListingDuration(60);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getDefaultListingValues();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(45);
    }

    @DisplayName("Duration in sessions should be correctly converted in minutes in override duration field")
    @Test
    void givenDurationAsSessionDurationShouldBeCorrectInOverrides() {
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingListingDuration(5);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(825);
    }

    @DisplayName("When duration in minutes and more than default value override fields should return it")
    @Test
    void giveDurationInMinutesShouldBeCorrectInOverrides() {
        int duration = 40;
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingListingDuration(duration);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(duration);
    }

    @DisplayName("When duration in minutes and less than default value override fields should return default value")
    @Test
    void giveDurationInMinutesLessThanDefaultOverridesShouldReturnDefaultValue() {
        int duration = 5;
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingListingDuration(duration);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        int expectedDefaultDuration = 60;
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(expectedDefaultDuration);
    }

    @Test
    void testNextHearingDateType() {
        var adjournment = sscsCaseData.getAdjournment();
        var date = LocalDate.parse("2040-12-24");
        adjournment.setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(date);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();
        var overrideFields = schedulingAndListingFields.getOverrideFields();
        var expectedDate = LocalDate.parse("2040-12-24");
        assertThat(overrideFields.getHearingWindow().getDateRangeStart()).isEqualTo(expectedDate);
    }

    @Test
    void hearingChannelShouldBeFromNextHearing() {
        var adjournment = sscsCaseData.getAdjournment();
        var hearingChannel = HearingChannel.TELEPHONE;
        var typeOfHearing = AdjournCaseTypeOfHearing.TELEPHONE;
        adjournment.setTypeOfNextHearing(typeOfHearing);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();
        assertThat(schedulingAndListingFields.getOverrideFields().getAppellantHearingChannel()).isEqualTo(hearingChannel);
    }

    @Test
    void testPEriod() {
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.TWENTY_EIGHT_DAYS);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();
        var overrideFields = schedulingAndListingFields.getOverrideFields();
        var expectedDate = LocalDate.now().plusDays(28);
        assertThat(overrideFields.getHearingWindow().getDateRangeStart()).isEqualTo(expectedDate);
    }

    @Test
    void givenCaseCanBeListedStraightAway_thenSetStateToReadyToList() {
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YesNo.YES);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getState()).isEqualTo(State.READY_TO_LIST);
    }

    @Test
    void givenCaseCannotBeListedStraightAway_thenSetStateToNotListable() {
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YesNo.NO);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getState()).isEqualTo(State.NOT_LISTABLE);
    }
}
