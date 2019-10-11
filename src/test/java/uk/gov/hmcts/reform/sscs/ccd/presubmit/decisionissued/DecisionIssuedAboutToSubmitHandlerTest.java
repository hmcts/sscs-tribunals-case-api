package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class DecisionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private DecisionIssuedAboutToSubmitHandler handler = new DecisionIssuedAboutToSubmitHandler();
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        initMocks(this);

        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .signedBy("User")
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .previewDocument(DocumentLink.builder()
                        .documentUrl(DOCUMENT_URL)
                        .documentBinaryUrl(DOCUMENT_URL + "/binary")
                        .documentFilename("decisionIssued.pdf")
                        .build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                .documentLink(sscsCaseData.getPreviewDocument())
                .documentDateAdded(LocalDate.now().minusDays(1).toString())
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .build()).build();


        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDecisionDocument() {
        sscsCaseData.setDecisionType(STRIKE_OUT.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());

        assertEquals(expectedDocument.getValue().getDocumentType(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
    }
}
