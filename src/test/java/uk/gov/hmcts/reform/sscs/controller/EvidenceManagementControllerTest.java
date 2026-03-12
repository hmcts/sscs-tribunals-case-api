package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceBase.DM_STORE_USER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@ExtendWith(MockitoExtension.class)
public class EvidenceManagementControllerTest {

    private static final String SERVICE_AUTH = "service-auth";
    private static final String SERVICE_NAME = "sscs";

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;

    @Mock
    private IdamService idamService;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private FileToPdfConversionService fileToPdfConversionService;

    private EvidenceManagementController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        openMocks(this);
        controller = new EvidenceManagementController(evidenceManagementService, evidenceManagementSecureDocStoreService, fileToPdfConversionService, false, null, objectMapper, authorisationService);
    }

    @Test
    public void shouldThrowEvidenceDocumentsMissingExceptionIfThereAreNoFilesInTheRequest() throws JsonProcessingException {
        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);

        assertThrows(EvidenceDocumentsMissingException.class, () -> controller.upload(SERVICE_AUTH, null));
    }

    @Test
    public void shouldThrowEvidenceDocumentsMissingExceptionForEmptyFileList() throws JsonProcessingException {
        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);

        assertThrows(EvidenceDocumentsMissingException.class, () -> controller.upload(SERVICE_AUTH, Collections.emptyList()));
    }

    @Test
    public void shouldUploadEvidenceDocumentList() throws JsonProcessingException {
        UploadResponse uploadResponse = mock(UploadResponse.class);
        UploadResponse.Embedded uploadResponseEmbedded = mock(UploadResponse.Embedded.class);

        when(uploadResponse.getEmbedded()).thenReturn(uploadResponseEmbedded);
        Document document = new Document();
        document.mimeType = "application/pdf";
        document.size = 656;
        document.originalDocumentName = "docname";
        Document.Links links = new Document.Links();
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
        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(SERVICE_AUTH, files);

        String json = "{\"documents\": [{\"classification\":null,\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":\"docname\",\"createdBy\":null,\"modifiedOn\":null,\"createdOn\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";

        verify(evidenceManagementService, times(1)).upload(files, DM_STORE_USER_ID);
        assertThat(actualUploadResponseEmbedded.getBody(), equalTo(json));
        verify(authorisationService).authenticate(SERVICE_AUTH);
        verify(authorisationService).allowOnlySscs(SERVICE_NAME);
    }

    @Test
    public void shouldUploadEvidenceDocumentListSecureDocStore() throws JsonProcessingException {
        controller = new EvidenceManagementController(evidenceManagementService, evidenceManagementSecureDocStoreService, fileToPdfConversionService, true, idamService, objectMapper, authorisationService);

        uk.gov.hmcts.reform.ccd.document.am.model.Document.Links links = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Links();
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
        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);

        when(evidenceManagementSecureDocStoreService.upload(files, idamTokens)).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(SERVICE_AUTH, files);

        String json = "{\"documents\": [{\"classification\":\"PUBLIC\",\"size\":656,\"mimeType\":\"application/pdf\",\"originalDocumentName\":\"docname\",\"createdOn\":null,\"modifiedOn\":null,\"createdBy\":null,\"lastModifiedBy\":null,\"ttl\":null,\"hashToken\":null,\"metadata\":null,\"_links\":{\"self\":{\"href\":\"selfURL\"},\"binary\":{\"href\":\"binaryUrl\"}}}]}";

        verify(evidenceManagementSecureDocStoreService, times(1)).upload(files, idamTokens);
        assertThat(actualUploadResponseEmbedded.getBody(), equalTo(json));
        verify(authorisationService).authenticate(SERVICE_AUTH);
        verify(authorisationService).allowOnlySscs(SERVICE_NAME);
    }

    @Test
    public void shouldUploadEvidenceDocumentListLogsParseException() throws JsonProcessingException {

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);

        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);
        when(fileToPdfConversionService.convert(files)).thenThrow(
            new FileToPdfConversionException("Conversion to PDF error", new RuntimeException()));

        assertThrows(FileToPdfConversionException.class, () -> controller.upload(SERVICE_AUTH, files));
    }

    @Test
    public void testToThrowForbiddenExceptionForUnauthorizedService() throws CcdException {
        String serviceAuth = "unauthorized-service-auth";
        String serviceName = "unauthorized-service";
        when(authorisationService.authenticate(serviceAuth)).thenReturn(serviceName);
        doThrow(new ForbiddenException("Service " + serviceName + " is not authorized for this action"))
                .when(authorisationService).allowOnlySscs(serviceName);

        assertThrows(ForbiddenException.class, () -> controller.upload(serviceAuth, null));
    }
}
