package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;

public class IssueAdjournmentNoticeAboutToSubmitHandlerMainTest extends IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {


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
        setupHearingDurationValues();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given an adjournment event with language interpreter set to no and case has existing interpreter, "
        + "then overwrite existing interpreter in hearing options")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterNotRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(NO);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter(YES.getValue())
            .languages("French")
            .build());
        setupHearingDurationValues();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(NO.getValue());
        assertThat(response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter().getIsInterpreterWanted()).isEqualTo(NO);
    }

    @DisplayName("Given an adjournment event with language interpreter required and interpreter language set, "
        + "then do not display error")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndLanguageSet_thenDoNotDisplayError() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));
        setupHearingDurationValues();

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
        setupHearingDurationValues();

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

        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingListingDuration(60);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        setupHearingDurationValues();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getDefaultListingValues();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(45);
    }

    @DisplayName("when hearing channel is updated in adjournment, update override from config")
    @Test
    void givenStandardDurationSelectedAndChannelUpdated_ShouldUpdateOverride() {

        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setTypeOfHearing(PAPER);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        setupHearingDurationValues();
        when(hearingDurationsService.addExtraTimeIfNeeded(any(), any(), any(), any())).thenReturn(90);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(90);
    }

    @DisplayName("when hearing channel is updated in adjournment, update override from config")
    @Test
    void givenStandardDurationSelectedAndInterpreterUpdated_ShouldUpdateOverride() {
        sscsCaseData.getSchedulingAndListingFields().setDefaultListingValues(OverrideFields.builder().duration(90).build());
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().duration(90).build());
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setInterpreterRequired(NO);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        setupHearingDurationValues();
        when(hearingDurationsService.addExtraTimeIfNeeded(any(), any(), any(), any())).thenReturn(60);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(60);
    }

    @DisplayName("Duration is set to null when we update channel to an in person hearing but cannot find a value in config")
    @Test
    void givenStandardDurationSelectedAndChannelUpdated_ShouldReturnNullWhenNoValueIsFound() {

        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setTypeOfHearing(PAPER);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationPaper(30);
        hearingDuration.setDurationFaceToFace(null);
        when(hearingDurationsService.getHearingDuration(eq(sscsCaseData.getBenefitCode()), eq(sscsCaseData.getIssueCode()))).thenReturn(hearingDuration);
        when(hearingDurationsService.addExtraTimeIfNeeded(any(), any(), any(), any())).thenReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields.getDuration()).isNull();
    }

    @DisplayName("override duration is kept when we don't update channel")
    @Test
    void givenStandardDurationSelectedAndChannelNotUpdated_ShouldKeepOverride() {
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().duration(85).build());
        var adjournment = sscsCaseData.getAdjournment();
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationFaceToFace(60);
        when(hearingDurationsService.getHearingDuration(eq(sscsCaseData.getBenefitCode()), eq(sscsCaseData.getIssueCode()))).thenReturn(hearingDuration);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();
        assertThat(overrideFields.getDuration()).isEqualTo(85);
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
        setupHearingDurationValues();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();
        var overrideFields = schedulingAndListingFields.getOverrideFields();
        var expectedDate = LocalDate.parse("2040-12-24");
        assertThat(overrideFields.getHearingWindow().getDateRangeStart()).isEqualTo(expectedDate);
    }

    @Test
    void hearingChannelShouldBeFromNextHearing() {
        setupHearingDurationValues();
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
        setupHearingDurationValues();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();
        var overrideFields = schedulingAndListingFields.getOverrideFields();
        var expectedDate = LocalDate.now().plusDays(28);
        assertThat(overrideFields.getHearingWindow().getDateRangeStart()).isEqualTo(expectedDate);
    }

    @Test
    void givenCaseCanBeListedStraightAway_thenSetStateToReadyToList() {
        setupHearingDurationValues();
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YesNo.YES);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getState()).isEqualTo(State.READY_TO_LIST);
    }

    @Test
    void givenCaseCannotBeListedStraightAway_thenSetStateToNotListable() {
        setupHearingDurationValues();
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YesNo.NO);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getState()).isEqualTo(State.NOT_LISTABLE);
    }
}
