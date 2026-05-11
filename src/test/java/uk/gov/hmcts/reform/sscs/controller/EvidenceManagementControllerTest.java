package uk.gov.hmcts.reform.sscs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceBase.DM_STORE_USER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@ExtendWith(MockitoExtension.class)
public class EvidenceManagementControllerTest {

    @Mock
    private EvidenceManagementService evidenceManagementService;
    @Mock
    private EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    @Mock
    private IdamService idamService;
    @Mock
    private FileToPdfConversionService fileToPdfConversionService;

    private EvidenceManagementController controller;

    @BeforeEach
    public void setUp() {
        controller = new EvidenceManagementController(evidenceManagementService,
                evidenceManagementSecureDocStoreService, fileToPdfConversionService, false, null);
    }

    @Test
    public void shouldThrowEvidenceDocumentsMissingExceptionIfThereAreNoFilesInTheRequest() {
        assertThrows(EvidenceDocumentsMissingException.class, () -> controller.upload(null));
    }

    @Test
    public void shouldThrowEvidenceDocumentsMissingExceptionForEmptyFileList() {
        assertThrows(EvidenceDocumentsMissingException.class, () -> controller.upload(Collections.emptyList()));
    }

    @Test
    public void shouldUploadEvidenceDocumentList() throws JsonProcessingException {
        final String jsonPayload = """
                {
                    "_embedded": {
                      "documents": [
                        {
                          "classification": null,
                          "size": 656,
                          "mimeType": "application/pdf",
                          "originalDocumentName": "docname",
                          "createdBy": null,
                          "modifiedOn": null,
                          "createdOn": null,
                          "_links": {
                            "self": {"href": "selfURL"},
                            "binary": {"href": "binaryUrl"}
                          }
                        }
                      ]
                    }
                }
                """;

        UploadResponse uploadResponse = new ObjectMapper().readValue(jsonPayload, UploadResponse.class);

        List<MultipartFile> files = List.of(new MockMultipartFile("file", "file".getBytes()));
        when(fileToPdfConversionService.convert(files)).thenReturn(files);
        when(evidenceManagementService.upload(files, DM_STORE_USER_ID)).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(files);

        verify(evidenceManagementService, times(1)).upload(files, DM_STORE_USER_ID);
        assertEquals(getJsonObj(jsonPayload).get("_embedded"), getJsonObj(actualUploadResponseEmbedded.getBody()));
    }

    @Test
    public void shouldUploadEvidenceDocumentListSecureDocStore() throws Exception {
        controller =
                new EvidenceManagementController(evidenceManagementService, evidenceManagementSecureDocStoreService,
                        fileToPdfConversionService, true, idamService);
        final String json = """
                  { "documents":
                    [
                        {
                          "classification": "PUBLIC",
                          "size": 656,
                          "mimeType": "application/pdf",
                          "originalDocumentName": "docname",
                          "createdOn": null,
                          "modifiedOn": null,
                          "createdBy": null,
                          "lastModifiedBy": null,
                          "ttl": null,
                          "hashToken": null,
                          "metadata": null,
                          "_links": {
                            "self": {"href": "selfURL"},
                            "binary": {"href": "binaryUrl"}
                          }
                        }
                    ]
                  }
                """;

        var links = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Links();
        links.binary = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        links.self = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        links.binary.href = "binaryUrl";
        links.self.href = "selfURL";

        var document = uk.gov.hmcts.reform.ccd.document.am.model.Document.builder()
                .size(656L)
                .mimeType("application/pdf")
                .originalDocumentName("docname")
                .classification(Classification.PUBLIC)
                .links(links)
                .build();

        var uploadResponse = new uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse(List.of(document));
        List<MultipartFile> files = List.of(new MockMultipartFile("file", "file".getBytes()));
        when(fileToPdfConversionService.convert(files)).thenReturn(files);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(evidenceManagementSecureDocStoreService.upload(files, idamTokens)).thenReturn(uploadResponse);

        ResponseEntity<String> actualUploadResponseEmbedded = controller.upload(files);


        verify(evidenceManagementSecureDocStoreService, times(1)).upload(files, idamTokens);
        assertEquals(getJsonObj(json), getJsonObj(actualUploadResponseEmbedded.getBody()));
    }

    @Test
    public void shouldUploadEvidenceDocumentListLogsParseException() {
        List<MultipartFile> files = List.of(new MockMultipartFile("file1", "file1".getBytes()));

        when(fileToPdfConversionService.convert(files)).thenThrow(
            new FileToPdfConversionException("Conversion to PDF error", new RuntimeException()));

        assertThrows(FileToPdfConversionException.class, () -> controller.upload(files));
    }

    private TreeNode getJsonObj(String expectedJson) throws JsonProcessingException {
        return new ObjectMapper().readTree(expectedJson);
    }
}
