package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.sscs.service.evidence.EvidenceManagementService;


public class EvidenceManagementControllerTest {

    @Mock
    private List<MultipartFile> files;

    @Mock
    private UploadResponse uploadResponse;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    private EvidenceManagementController controller;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        controller = new EvidenceManagementController(evidenceManagementService);
    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionIfThereAreNoFilesInTheRequest() {
        UploadResponse uploadResponse =  controller.uploadDocuments(null);
    }

    @Test(expected = EvidenceDocumentsMissingException.class)
    public void shouldThrowEvidenceDocumentsMissingExceptionForEmptyFileList() {
        UploadResponse uploadResponse =  controller.uploadDocuments(Collections.emptyList());
    }

    @Test
    public void shouldUploadEvidenceDocumentList() {
        Mockito.when(evidenceManagementService.upload(files)).thenReturn(uploadResponse);

        UploadResponse actualUploadResponse =  controller.uploadDocuments(files);

        verify(evidenceManagementService, times(1)).upload(files);
        assertThat(actualUploadResponse, equalTo(uploadResponse));

    }
}
