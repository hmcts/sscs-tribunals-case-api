package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

class AdjournCaseAboutToSubmitHandlerTest extends AdjournCaseAboutToSubmitHandlerTestBase {

    @Mock
    private AirLookupService airLookupService;

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

    @DisplayName("When interpreter required is not null and interpreter language is not selected "
        + ", then interpreter language set to NO")
    @Test
    void givenInterpreterRequiredIsNotNullButNoLanguageSelected_thenUpdateWithNoInterpreterLanguageAndRequiredFlagToYes() {
        DynamicList languagesList = new DynamicList(null);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(NO);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(languagesList);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(NO.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(NO.getValue());
    }

    @DisplayName("When interpreter required is null, then interpreter language set to null and requirement is NO")
    @Test
    void givenInterpreterRequiredIsNull_thenUpdateWithNullInterpreterLanguageAndRequiredFlagToYes() {
        DynamicList languagesList = new DynamicList(null);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(null);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(languagesList);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(NO.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(null);
    }

    @DisplayName("When adjournment is disabled and case is LA, then should not send any messages")
    @Test
    void givenFeatureFlagDisabled_thenNoMessageIsSent() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingMessageHelper);

        assertThat(response.getErrors()).isEmpty();
    }

    @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
        + "and no directions are being made, then should send a new hearing request in hearings API")
    @Test
    void givenCaseCannotBeListedRightAwayAndNoDirectionsBeingMade_thenNewHearingRequestSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndNoDirectionsGiven();

        assertHearingCreatedAndAdjournmentInProgress(response);
    }

    @DisplayName("When adjournment is enabled and case is LA and case can be listed right away "
        + "then should send a new hearing request in hearings API")
    @Test
    void givenCanBeListedRightAway_thenNewHearingRequestSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = canBeListed();

        assertHearingCreatedAndAdjournmentInProgress(response);
    }

    private void assertHearingCreatedAndAdjournmentInProgress(PreSubmitCallbackResponse<SscsCaseData> response) {
        verify(hearingMessageHelper, times(1))
            .sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getAdjournment().getAdjournmentInProgress()).isEqualTo(YES);
    }

    @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
        + "and directions are being made, then should not send any messages")
    @Test
    void givenCaseCannotBeListedRightAwayAndDirectionsAreBeingMade_thenNoMessagesSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndDirectionsGiven();

        verifyNoInteractions(hearingMessageHelper);

        assertThat(response.getErrors()).isEmpty();
    }

    @DisplayName("When we have changed the next hearing venue through an adjournment, show we change the region")
    @Test
    void givenAdjournCaseNextHearingVenueSelectedTrue_thenSetRegion() {
        String venueId = "185";

        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        String postcode = rpc.getPostcode();
        String processingVenue = "cardiff";

        BenefitType benefitType = BenefitType.builder().code("PIP").build();

        when(airLookupService.lookupAirVenueNameByPostCode(postcode, benefitType)).thenReturn(processingVenue);
        when(regionalProcessingCenterService.getByVenueId(venueId)).thenReturn(rpc);

        DynamicListItem venue = new DynamicListItem(venueId, null);
        DynamicList adjournedNextVenue = new DynamicList(venue, null);

        String originalRegion = "SUTTON";
        String originalProcessingVenue = "Staines";

        sscsCaseData.getAdjournment().setNextHearingVenueSelected(adjournedNextVenue);
        sscsCaseData.setRegion(originalRegion);
        sscsCaseData.setProcessingVenue(originalProcessingVenue);
        sscsCaseData.setAppeal(Appeal.builder()
            .appellant(Appellant.builder()
                .address(Address.builder().postcode(postcode).build()).isAppointee(YES.getValue())
                .build())
            .benefitType(benefitType)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(sscsCaseData.getRegion()).isEqualTo(rpc.getName());
        assertThat(sscsCaseData.getRegion()).isNotEqualTo(originalRegion);

        assertThat(sscsCaseData.getProcessingVenue()).isEqualTo(processingVenue);
        assertThat(sscsCaseData.getProcessingVenue()).isNotEqualTo(originalProcessingVenue);
    }

    @DisplayName("When we have changed the next hearing venue through an adjournment, but the region is null,"
        + " keep the original region and processing venue")
    @Test
    void givenRpcIsNull_thenDontSetRegion() {
        String venueId = "01010101010101";

        when(regionalProcessingCenterService.getByVenueId(venueId)).thenReturn(null);

        DynamicListItem venue = new DynamicListItem(venueId, null);
        DynamicList adjournedNextVenue = new DynamicList(venue, null);

        String originalRegion = "SUTTON";
        String originalProcessingVenue = "Staines";

        sscsCaseData.getAdjournment().setNextHearingVenueSelected(adjournedNextVenue);
        sscsCaseData.setRegion(originalRegion);
        sscsCaseData.setProcessingVenue(originalProcessingVenue);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(sscsCaseData.getRegion()).isEqualTo(originalRegion);
        assertThat(sscsCaseData.getProcessingVenue()).isEqualTo(originalProcessingVenue);
    }

    @DisplayName("When adjournment is enabled and case hearing type is Paper and Adjournment next hearing type is not provided "
            + ", then case hearing type should not be updated.")
    @Test
    void givenAdjournmentNextHearingNotProvided_thenNoChangeInHearingChannel() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.PAPER);
        callback.getCaseDetails().getCaseData().setHearings(Arrays.asList(new Hearing(hearingDetails)));
        callback.getCaseDetails().getCaseData().getAdjournment().setTypeOfNextHearing(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(callback.getCaseDetails().getCaseData().getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.PAPER);

    }

    @DisplayName("When adjournment is enabled and case hearing type is Paper and Adjournment next hearing type is Face To Face "
            + ", then case hearing type should updated from paper to face to face.")
    @Test
    void givenAdjournmentNextHearingIsFaceToFace_thenUpdateHearingChannel() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.PAPER);
        callback.getCaseDetails().getCaseData().setHearings(Arrays.asList(new Hearing(hearingDetails)));
        callback.getCaseDetails().getCaseData().getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(callback.getCaseDetails().getCaseData().getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.FACE_TO_FACE);
    }

    @DisplayName("When adjournment is enabled and case hearing type is face_to_face and Adjournment next hearing type is Paper "
            + ", then case hearing type should updated from face_to_face to Paper.")
    @Test
    void givenAdjournmentNextHearingIsPaper_thenUpdateHearingChannel() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.FACE_TO_FACE);
        callback.getCaseDetails().getCaseData().setHearings(Arrays.asList(new Hearing(hearingDetails)));
        callback.getCaseDetails().getCaseData().getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.PAPER);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(callback.getCaseDetails().getCaseData().getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.PAPER);
    }

}
