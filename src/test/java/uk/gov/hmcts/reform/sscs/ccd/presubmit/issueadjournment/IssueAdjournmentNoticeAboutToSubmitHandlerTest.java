package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.model.PoDetails;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

class IssueAdjournmentNoticeAboutToSubmitHandlerTest extends IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndTranslationRequired() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(TRANSLATION_REQUIRED));

        assertThat(sscsCaseData.getDwpState()).isNull();
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
        assertThat(sscsCaseData.getTranslationWorkOutstanding()).isEqualTo("Yes");
    }

    @Test
    void givenAnIssueAdjournmentEventWithDueDate_thenCreateAdjournmentWithGivenDueDate() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(1).toString());
    }

    @Test
    void givenAnIssueAdjournmentEventWithDueDateDaysOffset_thenCreateAdjournmentWithGivenDueDateOffset() {

        callback.getCaseDetails().getCaseData().getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(14).toString());
    }

    @Test
    void givenAnIssueAdjournmentEventWithCaseNotReadyToListRightAway_thenSetStateToNotListable() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getAdjournment().setCanCaseBeListedRightAway(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(NOT_LISTABLE);
    }

    @Test
    void givenAnIssueAdjournmentEventWithNoDirections_thenSetStateToReadyToList() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(READY_TO_LIST);
    }

    @Test
    void givenAnIssueAdjournmentEventForWelshCase0_thenTheCaseStateShouldStayUnchanged() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(HEARING);
    }

    @Test
    void givenAnIssueAdjournmentEventAndNoDraftAdjournmentOnCase_thenDisplayAnError() {

        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("There is no Draft Adjournment Notice on the case so adjournment cannot be issued");
    }

    @Test
    void givenANonPdfDecisionNotice_thenDisplayAnError() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getAdjournment().setPreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("You need to upload PDF documents only");


        assertThat(sscsCaseData.getSscsDocument())
            .map(SscsDocument::getValue)
            .map(SscsDocumentDetails::getDocumentType)
            .containsOnly(DRAFT_ADJOURNMENT_NOTICE.getValue());
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

        assertHearingCreatedAndAdjournmentInProgress(response, 0);
    }

    @DisplayName("When adjournment is enabled and case is LA and case can be listed right away "
        + "then should send a new hearing request in hearings API")
    @Test
    void givenCanBeListedRightAway_thenNewHearingRequestSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = canBeListed();

        assertHearingCreatedAndAdjournmentInProgress(response, 1);
    }

    private void assertHearingCreatedAndAdjournmentInProgress(PreSubmitCallbackResponse<SscsCaseData> response, int invocations) {
        verify(hearingMessageHelper, times(invocations))
            .sendListAssistCreateAdjournmentHearingMessage(sscsCaseData.getCcdCaseId());

        assertThat(response.getErrors()).isEmpty();
    }

    @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
        + "and directions are being made, then should not send any messages")
    @Test
    void givenCaseCannotBeListedRightAwayAndDirectionsAreBeingMade_thenNoMessagesSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndDirectionsGiven();

        verifyNoInteractions(hearingMessageHelper);

        assertThat(response.getErrors()).isEmpty();
    }

    @DisplayName("Given an adjournment event with language interpreter required and case has existing interpreter, "
        + "then overwrite existing interpreter in hearing options")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));
        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder()
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
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given an adjournment event without language interpreter required and interpreter language not set, "
            + "then set languages to no")
    @Test
    void givenAdjournmentEventWithNoLanguageInterpreterRequiredAndLanguageNotSet_thenUpdateOverrideFieldsToNo() {
        sscsCaseData.getAdjournment().setInterpreterRequired(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(NO.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isNull();
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
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(null);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.PAPER);

    }

    @DisplayName("When adjournment is enabled and case hearing type is Paper and Adjournment next hearing type is Face To Face "
        + ", then case hearing type should updated from paper to face to face.")
    @Test
    void givenAdjournmentNextHearingIsFaceToFace_thenUpdateHearingChannel() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.PAPER);
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.FACE_TO_FACE);
    }

    @DisplayName("When adjournment is enabled and case hearing type is face_to_face and Adjournment next hearing type is Paper "
        + ", then case hearing type should updated from face_to_face to Paper.")
    @Test
    void givenAdjournmentNextHearingIsPaper_thenUpdateHearingChannel() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.FACE_TO_FACE);
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(PAPER);
        when(regionalProcessingCenterService.getByVenueId("")).thenReturn(RegionalProcessingCenter.builder().epimsId("1111").build());
        when(venueService.getActiveRegionalEpimsIdsForRpc("1111")).thenReturn(List.of(VenueDetails.builder().epimsId("3456").build()));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getLatestHearing().getValue().getHearingChannel()).isEqualTo(HearingChannel.PAPER);
    }

    @DisplayName("When adjournment is enabled and theres a next hearing, then case hearing type should updated the wants to attend.")
    @ParameterizedTest
    @EnumSource(AdjournCaseTypeOfHearing.class)
    void givenAdjournmentNextHearing_thenUpdateWantsToAttend(AdjournCaseTypeOfHearing adjournCaseTypeOfHearing) {
        HearingDetails hearingDetails = new HearingDetails();
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(adjournCaseTypeOfHearing);

        if (PAPER.equals(adjournCaseTypeOfHearing)) {
            when(regionalProcessingCenterService.getByVenueId("")).thenReturn(RegionalProcessingCenter.builder().epimsId("1111").build());
            when(venueService.getActiveRegionalEpimsIdsForRpc("1111")).thenReturn(List.of(VenueDetails.builder().epimsId("3456").build()));
        }

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        HearingChannel hearingChannel = adjournCaseTypeOfHearing.getHearingChannel();
        if (HearingChannel.PAPER.equals(hearingChannel)) {
            assertThat(sscsCaseData.getAppeal().getHearingOptions().getWantsToAttend()).isEqualTo(NO.getValue());
            assertThat(sscsCaseData.getAppeal().getHearingType()).isEqualTo(HearingType.PAPER.getValue());
        } else {
            assertThat(sscsCaseData.getAppeal().getHearingOptions().getWantsToAttend()).isEqualTo(YES.getValue());
            assertThat(sscsCaseData.getAppeal().getHearingType()).isEqualTo(uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL.getValue());
        }

        assertThat(sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getAppellantHearingChannel()).isEqualTo(hearingChannel);
    }

    @DisplayName("When theres no latest hearing on the case, don't update the hearing type")
    @Test
    void givenNoLatestHearingOnCase_thenDontUpdateHearingType() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.PAPER);
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        sscsCaseData.setHearings(null);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getAppeal().getHearingType()).isEqualTo(uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL.getValue());
    }

    @DisplayName("When theres a latest hearing on the case with no value, don't update the hearing type")
    @Test
    void givenLatestHearingOnCaseWithNoValue_thenDontUpdateHearingType() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setHearingChannel(HearingChannel.PAPER);
        sscsCaseData.setHearings(List.of(new Hearing(hearingDetails)));
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        sscsCaseData.setHearings(List.of(Hearing.builder().build()));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getAppeal().getHearingType()).isEqualTo(uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL.getValue());
    }


    @DisplayName("When we have written an adjournment notice and excluded some panel members, and there are already excluded panel members, "
        + "add them to the existing excluded panel members list")
    @Test
    void givenPanelMembersExcluded_thenAddPanelMembersToExclusionList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .excludedPanelMembers(new ArrayList<>(Arrays.asList(
                new CollectionItem<>("1", JudicialUserBase.builder().idamId("1").build()),
                new CollectionItem<>("2", JudicialUserBase.builder().idamId("2").build())))).build());

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
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().personalCode("4").idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().personalCode("5").idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(2);
        assertThat(sscsCaseData.getSchedulingAndListingFields().getPanelMemberExclusions().getArePanelMembersExcluded())
            .isEqualTo(YES);
    }

    @DisplayName("When we have written an adjournment notice and reserved some panel members, and there are already reserved panel members, "
        + "add them to the existing reserved panel members list")
    @Test
    void givenPanelMembersReserved_thenAddPanelMembersToReservedList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .reservedPanelMembers(new ArrayList<>(Arrays.asList(
                new CollectionItem<>("1", JudicialUserBase.builder().idamId("1").build()),
                new CollectionItem<>("2", JudicialUserBase.builder().idamId("2").build())))).build());

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.RESERVED);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getReservedPanelMembers()).hasSize(3);
    }

    @DisplayName("When we have written an adjournment notice and not excluded some panel members, and there are already excluded panel members, "
        + "keep the existing excluded panel members list the same")
    @Test
    void givenPanelMembersNotExcluded_thenKeepExclusionListTheSame() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .excludedPanelMembers(new ArrayList<>(Arrays.asList(
                new CollectionItem<>("1", JudicialUserBase.builder().idamId("1").build()),
                new CollectionItem<>("2", JudicialUserBase.builder().idamId("2").build())))).build());

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(2);
    }

    @DisplayName("When we have written an adjournment notice and not excluded some panel members, and there are already excluded panel members, "
        + "keep the existing excluded panel members list the same")
    @Test
    void givenPanelMembersNotExcludedAndAdjournmentNotSelected_thenKeepExclusionListTheSame() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .excludedPanelMembers(new ArrayList<>(Arrays.asList(
                new CollectionItem<>("1", JudicialUserBase.builder().idamId("1").build()),
                new CollectionItem<>("2", JudicialUserBase.builder().idamId("2").build())))).build());

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.NO);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(2);
    }

    @DisplayName("")
    @Test
    void givenHearingTypeIsDataToBeFixedOrNull_thenHearingWindowShouldBeNull() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        Adjournment adjournment = sscsCaseData.getAdjournment();
        adjournment.setNextHearingFirstAvailableDateAfterDate(null);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(null);
        adjournment.setNextHearingDateOrPeriod(null);
        adjournment.setNextHearingDateType(null);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getHearingWindow()).isNull();
    }

    @Test
    void givenAdjournmentIsIssued_thenClearPoFields() {
        sscsCaseData.setPoAttendanceConfirmed(YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build());
        sscsCaseData.setPresentingOfficersHearingLink("link");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }
}
