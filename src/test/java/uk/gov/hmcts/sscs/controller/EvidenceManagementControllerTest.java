package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.sscs.service.EvidenceManagementService;


public class EvidenceManagementControllerTest {

    @Mock
    private EvidenceManagementService evidenceManagementService;

    private EvidenceManagementController controller;

    @Before
    public void setUp() {
        initMocks(this);
        controller = new EvidenceManagementController(evidenceManagementService);
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

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);

        UploadResponse uploadResponse = mock(UploadResponse.class);
        UploadResponse.Embedded uploadResponseEmbedded = mock(UploadResponse.Embedded.class);

        when(uploadResponse.getEmbedded()).thenReturn(uploadResponseEmbedded);
        when(evidenceManagementService.upload(files)).thenReturn(uploadResponse);

        UploadResponse.Embedded actualUploadResponseEmbedded = controller.upload(files);

        verify(evidenceManagementService, times(1)).upload(files);
        assertThat(actualUploadResponseEmbedded, equalTo(uploadResponseEmbedded));

    }
}
