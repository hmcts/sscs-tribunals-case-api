package uk.gov.hmcts.reform.sscs.ccd.presubmit.generatecoversheet;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;

@RunWith(JUnitParamsRunner.class)
public class GenerateCoversheetAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GenerateCoversheetAboutToStartHandler handler;
    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private CoversheetService coversheetService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        byte[] coversheetPdf = {2, 4, 6, 0, 1};
        SscsDocument sscsDocument = createSscsDocument();
        when(callback.getEvent()).thenReturn(EventType.GENERATE_COVERSHEET);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdCaseId").build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(pdfStoreService.storeDocument(any(), anyString(), any())).thenReturn(sscsDocument);
        when(coversheetService.createCoverSheet("ccdCaseId")).thenReturn(Optional.of(coversheetPdf));
        handler = new GenerateCoversheetAboutToStartHandler(coversheetService, pdfStoreService);
    }

    @Test
    public void canHandleCorrectly() {
        boolean actualResult = handler.canHandle(ABOUT_TO_START, callback);
        assertTrue(actualResult);
    }

    @Test
    public void canNotHandleCorrectly() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertFalse(actualResult);
    }

    @Test
    public void errorWhenNullDocument() {
        when(pdfStoreService.storeDocument(any(), anyString(), any())).thenReturn(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
    }

    @Test
    public void setsThePreviewDocumentField() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        DocumentLink documentLink = DocumentLink.builder().documentFilename("coversheet.pdf").documentBinaryUrl("urlLocation/binary").documentUrl("urlLocation").build();
        assertEquals(response.getData().getDocumentStaging().getPreviewDocument(), documentLink);
        verify(coversheetService).createCoverSheet(eq("ccdCaseId"));
        verify(pdfStoreService).storeDocument(any(), anyString(), any());
    }

    @Test
    public void givesErrorWhenCoversheetServiceFails() {
        when(coversheetService.createCoverSheet("ccdCaseId")).thenReturn(Optional.empty());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(response.getErrors().size(), 1);
    }

    private SscsDocument createSscsDocument() {
        SscsDocument sscsDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentUrl("urlLocation").build()).build()).build();
        return sscsDocument;
    }

    private static Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "urlLocation";
        links.self = link;
        document.links = links;
        return document;
    }
}
