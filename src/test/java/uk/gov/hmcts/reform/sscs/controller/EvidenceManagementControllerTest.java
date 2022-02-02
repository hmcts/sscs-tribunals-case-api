package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;


public class EvidenceManagementControllerTest {
    @Mock
    private EvidenceManagementSecureDocStoreService evidenceManagementService;

    @Mock
    private IdamService idamService;

    @Mock
    private FileToPdfConversionService fileToPdfConversionService;

    private EvidenceManagementController controller;

    @Before
    public void setUp() {
        openMocks(this);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        controller = new EvidenceManagementController(evidenceManagementService, fileToPdfConversionService, false, idamService);

    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionIfThereAreNoFilesInTheRequest() throws JsonProcessingException {
        controller.upload(null);
    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionForEmptyFileList() throws JsonProcessingException {
        controller.upload(Collections.emptyList());
    }

    @Test
    public void shouldUploadEvidenceDocumentList() throws JsonProcessingException {
        Document document = Document.builder().build();
        document.mimeType = "application/pdf";
        document.size = 656;
        document.originalDocumentName = "docname";
        Document.Links links = new Document.Links();;
        links.binary = new Document.Link();
        links.self = new Document.Link();
        links.binary.href = "binaryUrl";
        links.self.href = "selfURL";
        document.links = links;
        UploadResponse uploadResponse = mock(UploadResponse.class);

        when(uploadResponse.getDocuments()).thenReturn(Collections.singletonList(document));
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);
        when(fileToPdfConversionService.convert(files)).thenReturn(files);
        when(evidenceManagementService.upload(files, idamService.getIdamTokens())).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(files);

        //String json = "{\"documents\": [{\"classification\":null,\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":\"docname\",\"createdBy\":null,\"modifiedOn\":null,\"createdOn\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";
        String json = "{\"documents\": [{\"classification\":null,\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":\"docname\",\"createdOn\":null,\"ttl\":null,\"hashToken\":null,\"metadata\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";

        verify(evidenceManagementService, times(1)).upload(files, idamService.getIdamTokens());
        assertThat(actualUploadResponseEmbedded.getBody(), equalTo(json));
    }

    @Test
    public void shouldUploadEvidenceDocumentListSecureDocStore() throws JsonProcessingException {
        controller = new EvidenceManagementController(evidenceManagementService, fileToPdfConversionService, true, idamService);

        uk.gov.hmcts.reform.ccd.document.am.model.Document.Links links = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Links();;
        links.binary = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        links.self = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        links.binary.href = "binaryUrl";
        links.self.href = "selfURL";

        uk.gov.hmcts.reform.ccd.document.am.model.Document document =
                uk.gov.hmcts.reform.ccd.document.am.model.Document.builder()
                        .size(656L)
                        .mimeType("application/pdf")
                        .originalDocumentName("docname")
                        .classification(Classification.PUBLIC)
                        .links(links)
                        .build();

        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse uploadResponse =
                mock(uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse.class);
        when(uploadResponse.getDocuments()).thenReturn(Collections.singletonList(document));
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);
        when(fileToPdfConversionService.convert(files)).thenReturn(files);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(evidenceManagementService.upload(files, idamTokens)).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(files);

        String json = "{\"documents\": [{\"classification\":\"PUBLIC\",\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":\"docname\",\"createdOn\":null,\"ttl\":null,\"hashToken\":null,\"metadata\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";

        verify(evidenceManagementService, times(1)).upload(files, idamTokens);
        assertThat(actualUploadResponseEmbedded.getBody(), equalTo(json));
    }

    @Test(expected = FileToPdfConversionException.class)
    public void shouldUploadEvidenceDocumentListLogsParseException() throws JsonProcessingException {

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);

        when(fileToPdfConversionService.convert(files)).thenThrow(
            new FileToPdfConversionException("Conversion to PDF error", new RuntimeException()));

        controller.upload(files);
    }
}
