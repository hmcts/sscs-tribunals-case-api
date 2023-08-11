package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SOR_WRITE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class WriteStatementOfReasonsAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String STATEMENT_OF_REASONS_PDF = "Statement of Reasons.pdf";

    private WriteStatementOfReasonsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private SscsDocument expectedDocument;

    @Mock
    private FooterService footerService;

    @BeforeEach
    void setUp() {
        handler = new WriteStatementOfReasonsAboutToSubmitHandler(true, footerService);

        DocumentLink documentLink = DocumentLink.builder()
            .documentFilename(STATEMENT_OF_REASONS_PDF)
            .build();
        DocumentStaging documentStaging = DocumentStaging.builder()
            .previewDocument(documentLink)
            .build();
        Appeal appeal = Appeal.builder()
            .mrnDetails(MrnDetails.builder()
                .dwpIssuingOffice("3")
                .build())
            .build();
        caseData = SscsCaseData.builder()
            .appeal(appeal)
            .state(State.DORMANT_APPEAL_STATE)
            .ccdCaseId("1234")
            .documentStaging(documentStaging)
            .build();

        expectedDocument = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(documentLink)
                .documentFileName(documentLink.getDocumentFilename())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .build())
            .build();
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new WriteStatementOfReasonsAboutToSubmitHandler(false, footerService);
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError_whenPreviewDocFilenameIsCorrect() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        DocumentLink documentLink = DocumentLink.builder()
            .documentFilename(STATEMENT_OF_REASONS_PDF)
            .build();
        caseData.getDocumentStaging().setPreviewDocument(documentLink);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenStatementOfReasons_footerServiceIsCalledToCreateDocAndAddToBundle() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        String documentFilename = "Statement of Reasons.pdf";
        DocumentLink sorDoc = DocumentLink.builder()
            .documentFilename(documentFilename)
            .build();
        caseData.getDocumentStaging().setPreviewDocument(sorDoc);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        expectedDocument.getValue().setDocumentType(STATEMENT_OF_REASONS.getValue());
        expectedDocument.getValue().setDocumentLink(sorDoc);
        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
            eq(STATEMENT_OF_REASONS), any(), eq(null), eq(null), eq(null), eq(null));
    }

}
