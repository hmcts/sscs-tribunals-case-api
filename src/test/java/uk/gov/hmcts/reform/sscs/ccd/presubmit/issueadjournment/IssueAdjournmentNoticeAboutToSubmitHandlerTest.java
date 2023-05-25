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
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

class IssueAdjournmentNoticeAboutToSubmitHandlerTest extends IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getAdjournment().setPreviewDocument(DocumentLink.builder()
            .documentUrl("url")
            .documentFilename("adjournedcasedoc.pdf").build());
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
    void givenAnIssueAdjournmentEventWithDirectionsToAllParties_thenSetStateToNotListable() {

        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().getAdjournment().setPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getAdjournment().setAreDirectionsBeingMadeToParties(YES);

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
            .sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());

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
