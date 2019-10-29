package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class DecisionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";

    @Mock
    private FooterService footerService;

    private DecisionIssuedAboutToSubmitHandler handler;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        initMocks(this);

        handler = new DecisionIssuedAboutToSubmitHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(footerService.createFooterDocument(any(), any(), any(), any(), any(), any())).thenReturn(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.DECISION_NOTICE.getValue()).bundleAddition("A").documentLink(DocumentLink.builder().documentUrl("footerUrl").build()).build()).build());
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .signedBy("User")
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .sscsDocument(docs)
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
    public void willCopyThePreviewFileToTheInterlocDecisionDocumentAndAddFooter() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());

        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals("myTest.doc", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals(expectedDocument.getValue().getDocumentType(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        verify(footerService).createFooterDocument(eq(expectedDocument.getValue().getDocumentLink()), eq("Decision notice"), eq("A"), any(), any(), eq(DocumentType.DECISION_NOTICE));
    }

    @Test
    public void givenManuallyUploadedDecisionDocument_thenOverwriteOriginalWithFooterDocument() {
        sscsCaseData.setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL).build())
                .documentDateAdded(LocalDate.now().toString()).build()).build();

        sscsDocuments.add(theDocument);
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("footerUrl", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void given2ManuallyUploadedDecisionDocumentsOneWithNoDate_thenIssueDocumentWithNoDate() {
        sscsCaseData.setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL).build()).build())
                .build();

        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL2).build())
                .documentDateAdded(LocalDate.now().toString()).build()).build();

        sscsDocuments.add(document1);
        sscsDocuments.add(document2);
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterDocument(eq(document1.getValue().getDocumentLink()), eq("Decision notice"), eq("A"), any(), any(), eq(DocumentType.DECISION_NOTICE));
        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("footerUrl", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void given2ManuallyUploadedDecisionDocumentsWithNoDates_thenIssueLastDocumentWithNoDate() {
        sscsCaseData.setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL).build()).build())
                .build();

        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL2).build()).build())
                .build();

        sscsDocuments.add(document1);
        sscsDocuments.add(document2);
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterDocument(eq(document2.getValue().getDocumentLink()), eq("Decision notice"), eq("A"), any(), any(), eq(DocumentType.DECISION_NOTICE));
        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("footerUrl", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }
}
