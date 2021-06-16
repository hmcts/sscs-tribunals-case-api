package uk.gov.hmcts.reform.sscs.controller;

import static uk.gov.hmcts.reform.sscs.service.SubmitAppealService.DM_STORE_USER_ID;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@RestController
@Slf4j
public class EvidenceManagementController {

    private final EvidenceManagementService evidenceManagementService;
    private final FileToPdfConversionService fileToPdfConversionService;

    @Autowired
    public EvidenceManagementController(EvidenceManagementService evidenceManagementService,
                                        FileToPdfConversionService fileToPdfConversionService) {
        this.evidenceManagementService = evidenceManagementService;
        this.fileToPdfConversionService = fileToPdfConversionService;
    }

    @ApiOperation(value = "Upload additional evidence converted to PDF",
        notes = "Uploads evidence for an appeal which will be held in a draft state against the case that is not "
            + "visible by a caseworker in CCD. You will need to submit the evidence for it to be visbale in CCD "
            + "by a caseworker. You need to have an appeal in CCD."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Evidence has been added to the appeal"),
    })
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

        try {
            List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(files);
            return evidenceManagementService
                .upload(convertedFiles, DM_STORE_USER_ID)
                .getEmbedded();

        } catch (FileToPdfConversionException e) {
            log.error("Error while converting files for evidence upload: " + e.getMessage());
            throw e;
        }

    }

}
