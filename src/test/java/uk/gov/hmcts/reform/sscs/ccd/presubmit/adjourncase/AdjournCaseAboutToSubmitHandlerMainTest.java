package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

class AdjournCaseAboutToSubmitHandlerMainTest extends AdjournCaseAboutToSubmitHandlerTestBase {

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

    @DisplayName("When a previous write adjournment notice in place and you call the event the second time the generated date needs to be updated so its reflected in the issue adjournment event")
    @Test
    void givenPreviousWritenAdjournCaseTriggerAnotherThenCheckIssueAdjournmentHasMostRecentDate() {
        sscsCaseData.getAdjournment().setGeneratedDate(LocalDate.parse("2023-01-01"));
        assertThat(sscsCaseData.getAdjournment().getGeneratedDate()).isEqualTo(LocalDate.parse("2023-01-01"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        LocalDate date = response.getData().getAdjournment().getGeneratedDate();
        assertThat(date).isEqualTo(LocalDate.now());
    }
}
