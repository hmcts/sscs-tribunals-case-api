package uk.gov.hmcts.reform.sscs.ccd.presubmit.generatecoversheet;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;

@RunWith(JUnitParamsRunner.class)
public class GenerateCoversheetAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GenerateCoversheetAboutToStartHandler handler;
    @Mock
    private EvidenceManagementService evidenceManagementService;

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
        UploadResponse uploadResponse = createUploadResponse();
        when(callback.getEvent()).thenReturn(EventType.GENERATE_COVERSHEET);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdCaseId").build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);
        when(coversheetService.createCoverSheet("ccdCaseId")).thenReturn(Optional.of(coversheetPdf));
        handler = new GenerateCoversheetAboutToStartHandler(coversheetService, evidenceManagementService);
    }

    @Test
    public void canHandleCorrectly() {
        boolean actualResult = handler.canHandle(ABOUT_TO_START, callback);
        assertTrue(actualResult);
    }

    @Test
    public void setsThePreviewDocumentField() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        DocumentLink documentLink = DocumentLink.builder().documentFilename("coversheet.pdf").documentBinaryUrl("urlLocation/binary").documentUrl("urlLocation").build();
        assertEquals(response.getData().getPreviewDocument(), documentLink);
        verify(coversheetService).createCoverSheet(eq("ccdCaseId"));
        verify(evidenceManagementService).upload(anyList(), anyString());
    }

    @Test
    public void givesErrorWhenCoversheetServiceFails() {
        when(coversheetService.createCoverSheet("ccdCaseId")).thenReturn(Optional.empty());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(response.getErrors().size(), 1);
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(singletonList(document));
        return response;
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
