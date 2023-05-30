package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
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

    @DisplayName("When we have written an adjournment notice and excluded some panel members, and there are already excluded panel members, "
        + "add them to the existing excluded panel members list")
    @Test
    void givenPanelMembersExcluded_thenAddPanelMembersToExclusionList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .excludedPanelMembers(new ArrayList<>(Arrays.asList(
                new CcdValue<>(JudicialUserBase.builder().idamId("1").build()),
                new CcdValue<>(JudicialUserBase.builder().idamId("2").build())))).build());

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(3);
    }

    @DisplayName("When we have written an adjournment notice and excluded some panel members, and there are no current "
        + "exclusions, add them to the excluded panel members list")
    @Test
    void givenNoExistingPanelMembersExcluded_thenAddPanelMembersToExclusionList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(2);
        assertThat(sscsCaseData.getSchedulingAndListingFields().getPanelMemberExclusions().getArePanelMembersExcluded())
            .isEqualTo(YES);
    }

}
