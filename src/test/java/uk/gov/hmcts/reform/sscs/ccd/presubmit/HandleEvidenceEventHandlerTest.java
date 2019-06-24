package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class HandleEvidenceEventHandlerTest {

    private HandleEvidenceEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    List<ScannedDocument> scannedDocumentList = new ArrayList<>();

    @Before
    public void setUp() {
        initMocks(this);
        handler = new HandleEvidenceEventHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                    .fileName("bla.pdf")
                    .subtype("sscs1")
                    .url(DocumentLink.builder().documentUrl("www.test.com").build())
                    .scannedDate("2019-06-12T00:00:00.000")
                    .controlNumber("123")
                    .build()).build();

        scannedDocumentList.add(scannedDocument);
        sscsCaseData = SscsCaseData.builder().scannedDocuments(scannedDocumentList).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenACaseWithScannedDocuments_thenMoveToSscsDocuments() {

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("bla.pdf", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("sscs1", response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("www.test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("123", response.getData().getSscsDocument().get(0).getValue().getControlNumber());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType("appellantEvidence").documentFileName("exist.pdf").build()).build();
        sscsDocuments.add(doc);

        sscsCaseData = SscsCaseData.builder().scannedDocuments(scannedDocumentList).sscsDocument(sscsDocuments).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("exist.pdf", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("bla.pdf", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData = SscsCaseData.builder().build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No further evidence to process", error);
        }
    }

}