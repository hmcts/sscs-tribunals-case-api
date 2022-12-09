package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

class IssueAdjournmentNoticeAboutToSubmitHandlerTest extends IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndSetStatesAndClearDraftDoc_andSetAdjournmentInProgressToNoIfFeatureFlagEnabled() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true); // TODO SSCS-10951

        final Adjournment adjournment = confirmAdjournment();

        assertThat(adjournment).hasAllNullFieldsOrPropertiesExcept("adjournmentInProgress");
        assertThat(adjournment.getAdjournmentInProgress()).isEqualTo(NO);
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndSetStatesAndClearDraftDoc() { // TODO SSCS-10951
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);

        final Adjournment adjournment = confirmAdjournment();

        assertThat(adjournment).hasAllNullFieldsOrProperties();
    }

    @NotNull
    private Adjournment confirmAdjournment() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        final Adjournment adjournment = callback.getCaseDetails().getCaseData().getAdjournment();
        adjournment.setPreviewDocument(docLink);
        adjournment.setDirectionsDueDate(LocalDate.now().plusDays(1));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(null));

        assertThat(sscsCaseData.getDwpState()).isEqualTo(DwpState.ADJOURNMENT_NOTICE_ISSUED.getId());
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(1).toString());

        assertThat(sscsCaseData.getSscsDocument())
            .map(SscsDocument::getValue)
            .map(SscsDocumentDetails::getDocumentType)
            .doesNotContain(DRAFT_ADJOURNMENT_NOTICE.getValue());

        assertThat(sscsCaseData.getSscsDocument().stream()
            .filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()))).isEmpty();
        return adjournment;
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
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION.getId());
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

}
