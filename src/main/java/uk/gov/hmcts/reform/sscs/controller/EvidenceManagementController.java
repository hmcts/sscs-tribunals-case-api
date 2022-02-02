package uk.gov.hmcts.reform.sscs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@RestController
@Slf4j
public class EvidenceManagementController {

    private final EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    private final FileToPdfConversionService fileToPdfConversionService;
    private final boolean secureDocStoreEnabled;
    private IdamService idamService;

    @Autowired
    public EvidenceManagementController(EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService,
                                        FileToPdfConversionService fileToPdfConversionService,
                                        @Value("${feature.secure-doc-store.enabled:false}") boolean secureDocStoreEnabled,
                                        IdamService idamService) {
        this.evidenceManagementSecureDocStoreService = evidenceManagementSecureDocStoreService;
        this.fileToPdfConversionService = fileToPdfConversionService;
        this.secureDocStoreEnabled = secureDocStoreEnabled;
        this.idamService = idamService;
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
    public ResponseEntity<String> upload(
        @RequestParam("file") List<MultipartFile> files
    ) throws JsonProcessingException {
        if (null == files || files.isEmpty()) {
            throw new EvidenceDocumentsMissingException();
        }

        try {
            List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(files);

            List<Document> documents = evidenceManagementSecureDocStoreService
                    .upload(convertedFiles, idamService.getIdamTokens()).getDocuments();
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonText =  "{\"documents\": " + objectMapper.writeValueAsString(documents) + "}";
            return ResponseEntity.ok(jsonText);

        } catch (FileToPdfConversionException e) {
            log.error("Error while converting files for evidence upload: " + e.getMessage());
            throw e;
        }
    }

}
