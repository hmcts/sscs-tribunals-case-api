package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

public class DwpUploadFunctionalTest extends AbstractFunctionalTest {
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    public DwpUploadFunctionalTest() {
        super();
    }

    // Need tribunals running to pass this functional test
    @Test
    public void dwpUploadResponseEventSendsToReadyToList() throws IOException {

        SscsCaseDetails createdCase = createCaseWithState(CREATE_TEST_CASE, "UC", "Universal Credit", State.READY_TO_LIST.getId());

        //Get case into correct state without triggering any callbacks that cause race conditions
        updateCaseEvent(SEND_TO_DWP_OFFLINE, createdCase);

        String json = getJson(DWP_UPLOAD_RESPONSE.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        String documentUrl = uploadDocToDocMgmtStore();
        String binaryUrl = documentUrl + "/binary";
        json = json.replace("DWP_AT38_DOCUMENT_URL", documentUrl);
        json = json.replace("DWP_AT38_BINARY_DOCUMENT_URL", binaryUrl);
        json = json.replace("DWP_RESPONSE_DOCUMENT_URL", documentUrl);
        json = json.replace("DWP_RESPONSE_BINARY_DOCUMENT_URL", binaryUrl);
        json = json.replace("DWP_EVIDENCE_DOCUMENT_URL", documentUrl);
        json = json.replace("DWP_EVIDENCE_BINARY_DOCUMENT_URL", binaryUrl);

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertEquals("readyToList", caseDetails.getState());
    }

    private String uploadDocToDocMgmtStore() throws IOException {
        Path evidencePath = new File(Objects.requireNonNull(
            getClass().getClassLoader().getResource(EVIDENCE_DOCUMENT_PDF)).getFile()).toPath();

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
            .content(Files.readAllBytes(evidencePath))
            .name(EVIDENCE_DOCUMENT_PDF)
            .contentType(APPLICATION_PDF)
            .build();

        UploadResponse upload = evidenceManagementService.upload(singletonList(file), "sscs");

        return upload.getEmbedded().getDocuments().get(0).links.self.href;
    }


}
