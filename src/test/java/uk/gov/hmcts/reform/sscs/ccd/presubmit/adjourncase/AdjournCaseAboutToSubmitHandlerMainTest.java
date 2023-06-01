package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

class AdjournCaseAboutToSubmitHandlerMainTest extends AdjournCaseAboutToSubmitHandlerTestBase {

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
    @Test
    void givenAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                    .documentFileName(OLD_DRAFT_DOC)
                    .documentType(DRAFT_ADJOURNMENT_NOTICE.getValue())
                    .build())
            .build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, null);
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

    @DisplayName("When a previous write adjournment notice in place and you call the event the second time the generated date needs to be updated so its reflected in the issue adjournment event")
    @Test
    void givenPreviousWritenAdjournCaseTriggerAnotherThenCheckIssueAdjournmentHasMostRecentDate() {
        sscsCaseData.getAdjournment().setGeneratedDate(LocalDate.parse("2023-01-01"));
        assertThat(sscsCaseData.getAdjournment().getGeneratedDate()).isEqualTo(LocalDate.parse("2023-01-01"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        LocalDate date = response.getData().getAdjournment().getGeneratedDate();
        assertThat(date).isEqualTo(LocalDate.now());
    }

    @DisplayName("OverrideFields should persist correct epimsid for selected venue in adjournment")
    @Test
    void givenVenueReturnCorrectEpimsInOverrideFields() {
        String epimsId = "epimsId";
        String code = "venueCode";
        VenueDetails details = VenueDetails.builder()
            .epimsId(epimsId)
            .build();

        var item = new DynamicListItem(code, code);
        var list = new DynamicList(item, null);

        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        when(venueDataLoader.getVenueDetailsMap()).thenReturn(Map.of(code, details));
        when(regionalProcessingCenterService.getByVenueId(code)).thenReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        var schedulingAndListingFields = response.getData().getSchedulingAndListingFields();
        assertThat(schedulingAndListingFields).isNotNull();

        var overrideFields = schedulingAndListingFields.getOverrideFields();
        assertThat(overrideFields).isNotNull();

        var hearingEpimsIds = overrideFields.getHearingVenueEpimsIds();
        assertThat(hearingEpimsIds).isNotNull().hasSize(1).allMatch(b -> b.getValue().getValue().equals(epimsId));
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
        int expectedDefaultDuration = 30;
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
        var expectedDate = LocalDate.parse("2040-12-25").atStartOfDay();
        assertThat(overrideFields.getHearingWindow().getFirstDateTimeMustBe()).isEqualTo(expectedDate);
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
        var expectedDate = LocalDate.now().plusDays(28).atStartOfDay();
        assertThat(overrideFields.getHearingWindow().getFirstDateTimeMustBe()).isEqualTo(expectedDate);
    }
}
