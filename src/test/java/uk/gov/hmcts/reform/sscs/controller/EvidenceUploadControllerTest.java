package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;
import uk.gov.hmcts.reform.sscs.service.evidence.EvidenceUploadService;

public class EvidenceUploadControllerTest {

    private EvidenceUploadService evidenceUploadService;
    private EvidenceUploadController evidenceUploadController;
    private String someOnlineHearingId;
    private CoversheetService coversheetService;

    @Before
    public void setUp() {
        evidenceUploadService = mock(EvidenceUploadService.class);
        coversheetService = mock(CoversheetService.class);
        evidenceUploadController = new EvidenceUploadController(evidenceUploadService, coversheetService);
        someOnlineHearingId = "someOnlineHearingId";
    }

    @Test
    public void submitEvidence() {
        MultipartFile file = mock(MultipartFile.class);
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitHearingEvidence(someOnlineHearingId, description, file)).thenReturn(true);

        ResponseEntity responseEntity = evidenceUploadController.submitSingleEvidence(someOnlineHearingId, file, "some description", "idamEmail");

        assertThat(responseEntity.getStatusCode(), is(NO_CONTENT));
    }

    @Test
    public void submitEvidenceWhenHearingDoesNotExist() {
        MultipartFile file = mock(MultipartFile.class);
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitHearingEvidence(someOnlineHearingId, description, file)).thenReturn(false);

        ResponseEntity responseEntity = evidenceUploadController.submitSingleEvidence(someOnlineHearingId, file, "some description", "idamEmail");

        assertThat(responseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void getCoversheet() {
        byte[] coversheetPdf = {2, 4, 6, 0, 1};
        when(coversheetService.createCoverSheet(someOnlineHearingId)).thenReturn(Optional.of(coversheetPdf));
        ResponseEntity<ByteArrayResource> response = evidenceUploadController.getCoverSheet(someOnlineHearingId);

        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getHeaders().getContentType(), is(MediaType.APPLICATION_PDF));
        assertThat(response.getHeaders().getContentDisposition().getFilename(), is("evidence_cover_sheet.pdf"));
        assertThat(response.getBody().getByteArray(), is(coversheetPdf));
    }

    @Test
    public void getCoversheetWhenHearingDoesNotExist() {
        when(coversheetService.createCoverSheet(someOnlineHearingId)).thenReturn(Optional.empty());
        ResponseEntity<ByteArrayResource> response = evidenceUploadController.getCoverSheet(someOnlineHearingId);

        assertThat(response.getStatusCode(), is(NOT_FOUND));
    }
}
