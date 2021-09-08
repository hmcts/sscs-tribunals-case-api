package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.service.SubmitAppealService.DM_STORE_USER_ID;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;


public class EvidenceManagementControllerTest {

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private FileToPdfConversionService fileToPdfConversionService;

    private EvidenceManagementController controller;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new EvidenceManagementController(evidenceManagementService, null, fileToPdfConversionService, false, null);
    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionIfThereAreNoFilesInTheRequest() {
        controller.upload(null);
    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionForEmptyFileList() {
        controller.upload(Collections.emptyList());
    }

    @Test
    public void shouldUploadEvidenceDocumentList() {
        UploadResponse uploadResponse = mock(UploadResponse.class);
        UploadResponse.Embedded uploadResponseEmbedded = mock(UploadResponse.Embedded.class);

        when(uploadResponse.getEmbedded()).thenReturn(uploadResponseEmbedded);
        Document document = new Document();
        document.mimeType = "application/pdf";
        document.size = 656;
        Document.Links links = new Document.Links();;
        links.binary = new Document.Link();
        links.self = new Document.Link();
        links.binary.href = "binaryUrl";
        links.self.href = "selfURL";
        document.links = links;

        when(uploadResponse.getEmbedded().getDocuments()).thenReturn(Collections.singletonList(document));
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);
        when(fileToPdfConversionService.convert(files)).thenReturn(files);
        when(evidenceManagementService.upload(files, DM_STORE_USER_ID)).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(files);

        String json = "{\"documents\": [{\"classification\":null,\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":null,\"createdBy\":null,\"modifiedOn\":null,\"createdOn\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";

        verify(evidenceManagementService, times(1)).upload(files, DM_STORE_USER_ID);
        assertThat(actualUploadResponseEmbedded.getBody(), equalTo(json));
    }

    @Test(expected = FileToPdfConversionException.class)
    public void shouldUploadEvidenceDocumentListLogsParseException() {

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);

        when(fileToPdfConversionService.convert(files)).thenThrow(
            new FileToPdfConversionException("Conversion to PDF error", new RuntimeException()));

        controller.upload(files);
    }
}
