package uk.gov.hmcts.reform.sscs.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@RestController
public class EvidenceManagementController {

    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    public EvidenceManagementController(EvidenceManagementService evidenceManagementService) {
        this.evidenceManagementService = evidenceManagementService;
    }

    @PostMapping(
        value = "/evidence/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public UploadResponse.Embedded upload(
        @RequestParam("file") List<MultipartFile> files
    ) {
        if (null == files || files.isEmpty()) {
            throw new EvidenceDocumentsMissingException();
        }

        return evidenceManagementService
            .upload(files, "sscs")
            .getEmbedded();
    }

}
