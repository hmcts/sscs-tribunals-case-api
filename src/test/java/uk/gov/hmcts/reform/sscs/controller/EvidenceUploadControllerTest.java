package uk.gov.hmcts.reform.sscs.controller;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Evidence;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;
import uk.gov.hmcts.reform.sscs.service.AppellantStatementService;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;
import uk.gov.hmcts.reform.sscs.service.evidence.EvidenceUploadService;
import uk.gov.hmcts.reform.sscs.service.pdf.MyaEventActionContext;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.IllegalFileTypeException;

@ExtendWith(MockitoExtension.class)
public class EvidenceUploadControllerTest {

    @Mock
    private EvidenceUploadService evidenceUploadService;
    @Mock
    private AppellantStatementService appellantStatementService;
    @Mock
    private CoversheetService coversheetService;

    private String someOnlineHearingId;
    private String someEvidenceId;
    private Evidence evidence;
    private String onlineHearingId;
    private Statement statement;

    private EvidenceUploadController evidenceUploadController;

    @BeforeEach
    public void setUp() {
        someOnlineHearingId = "someOnlineHearingId";
        someEvidenceId = "someEvidenceId";
        onlineHearingId = "someOnlineHearingId";
        statement = new Statement("someStatement", "someTya");
        evidence = new Evidence(someEvidenceId, "someFileName", "someFileUrl");

        evidenceUploadController =
                new EvidenceUploadController(evidenceUploadService, coversheetService, appellantStatementService);
    }

    @Test
    public void canUploadEvidence() {
        MultipartFile file = mock(MultipartFile.class);
        when(evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file)).thenReturn(of(evidence));

        var evidenceResponseEntity = evidenceUploadController.uploadEvidence(someOnlineHearingId, file);

        assertThat(evidenceResponseEntity.getStatusCode(), is(OK));
    }

    @Test
    public void cannotUploadEvidenceWhenOnlineHearingDoesNotExist() {
        MultipartFile file = mock(MultipartFile.class);
        when(evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file)).thenReturn(empty());

        var evidenceResponseEntity = evidenceUploadController.uploadEvidence(someOnlineHearingId, file);

        assertThat(evidenceResponseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void cannotUploadDocumentsThatDocumentStoreDoesNotSupport() {
        MultipartFile file = mock(MultipartFile.class);
        when(evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file))
                .thenThrow(new IllegalFileTypeException("someFile.bad"));

        var evidenceResponseEntity = evidenceUploadController.uploadEvidence(someOnlineHearingId, file);

        assertThat(evidenceResponseEntity.getStatusCode(), is(UNPROCESSABLE_ENTITY));
    }

    @Test
    public void submitEvidence() {
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitHearingEvidence(someOnlineHearingId, description)).thenReturn(true);

        var responseEntity = evidenceUploadController.submitEvidence(someOnlineHearingId, description);

        assertThat(responseEntity.getStatusCode(), is(NO_CONTENT));
    }

    @Test
    public void submitSingleAudioVideoEvidence() {
        MultipartFile file = mock(MultipartFile.class);
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, description, file))
                .thenReturn(true);

        var responseEntity = evidenceUploadController
                .submitSingleEvidence(someOnlineHearingId, file, "some description", "idamEmail");

        assertThat(responseEntity.getStatusCode(), is(NO_CONTENT));
    }

    @Test
    public void submitEvidenceWhenHearingDoesNotExist() {
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitHearingEvidence(someOnlineHearingId, description)).thenReturn(false);

        var responseEntity = evidenceUploadController.submitEvidence(someOnlineHearingId, description);

        assertThat(responseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void submitSingleAudioVideoEvidenceWhenHearingDoesNotExist() {
        MultipartFile file = mock(MultipartFile.class);
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, description, file))
                .thenReturn(false);

        var responseEntity = evidenceUploadController
                .submitSingleEvidence(someOnlineHearingId, file, "some description", "idamEmail");

        assertThat(responseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void submitSingleEvidenceWhenHearingDoesNotExist() {
        MultipartFile file = mock(MultipartFile.class);
        EvidenceDescription description = new EvidenceDescription("some description", "idamEmail");
        when(evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, description, file))
                .thenReturn(false);

        var responseEntity = evidenceUploadController
                .submitSingleEvidence(someOnlineHearingId, file, "some description", "idamEmail");

        assertThat(responseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void listEvidence() {
        List<Evidence> expectedEvidence = singletonList(evidence);
        when(evidenceUploadService.listDraftHearingEvidence(someOnlineHearingId)).thenReturn(expectedEvidence);

        ResponseEntity<List<Evidence>> listResponseEntity =
                evidenceUploadController.listDraftEvidence(someOnlineHearingId);

        assertThat(listResponseEntity.getStatusCode(), is(OK));
        assertThat(listResponseEntity.getBody(), is(expectedEvidence));
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

    @Test
    public void canDeleteEvidence() {
        when(evidenceUploadService.deleteDraftEvidence(someOnlineHearingId, someEvidenceId)).thenReturn(true);

        var evidenceResponseEntity = evidenceUploadController
                .deleteEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(evidenceResponseEntity.getStatusCode(), is(NO_CONTENT));
    }

    @Test
    public void cannotDeleteEvidenceWhenOnlineHearingDoesNotExist() {
        when(evidenceUploadService.deleteDraftEvidence(someOnlineHearingId, someEvidenceId)).thenReturn(false);

        var evidenceResponseEntity = evidenceUploadController
                .deleteEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(evidenceResponseEntity.getStatusCode(), is(NOT_FOUND));
    }

    @Test
    public void canUploadAStatement() {
        when(appellantStatementService.handleAppellantStatement(onlineHearingId, statement))
                .thenReturn(Optional.of(mock(MyaEventActionContext.class)));

        var responseEntity = evidenceUploadController.uploadStatement(onlineHearingId, statement);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.NO_CONTENT));
    }

    @Test
    public void cannotUploadAStatementIfOnlineHearingNotFound() {
        when(appellantStatementService.handleAppellantStatement(onlineHearingId, statement))
                .thenReturn(Optional.empty());

        var responseEntity = evidenceUploadController.uploadStatement(onlineHearingId, statement);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
}
