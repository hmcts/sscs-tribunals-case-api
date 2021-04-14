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
import org.springframework.web.multipart.MultipartFile;
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
        controller = new EvidenceManagementController(evidenceManagementService, fileToPdfConversionService);
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
        when(fileToPdfConversionService.convert(files)).thenReturn(files);
        when(evidenceManagementService.upload(files, DM_STORE_USER_ID)).thenReturn(uploadResponse);

        UploadResponse.Embedded actualUploadResponseEmbedded = controller.upload(files);

        verify(evidenceManagementService, times(1)).upload(files, DM_STORE_USER_ID);
        assertThat(actualUploadResponseEmbedded, equalTo(uploadResponseEmbedded));

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
