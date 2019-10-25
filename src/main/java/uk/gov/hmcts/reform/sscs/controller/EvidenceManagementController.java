package uk.gov.hmcts.reform.sscs.controller;

import static uk.gov.hmcts.reform.sscs.service.SubmitAppealService.DM_STORE_USER_ID;

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
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@RestController
public class EvidenceManagementController {

    private final EvidenceManagementService evidenceManagementService;
    private final FileToPdfConversionService fileToPdfConversionService;

    @Autowired
    public EvidenceManagementController(EvidenceManagementService evidenceManagementService, FileToPdfConversionService fileToPdfConversionService) {
        this.evidenceManagementService = evidenceManagementService;
        this.fileToPdfConversionService = fileToPdfConversionService;
    }

    @PostMapping(
        value = "/evidence/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UploadResponse.Embedded upload(
        @RequestParam("file") List<MultipartFile> files
    ) {
        if (null == files || files.isEmpty()) {
            throw new EvidenceDocumentsMissingException();
        }

        List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(files);

        return evidenceManagementService
            .upload(convertedFiles, DM_STORE_USER_ID)
            .getEmbedded();
    }

}
