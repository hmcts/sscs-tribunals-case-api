package uk.gov.hmcts.reform.sscs.controller;

import static uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceBase.DM_STORE_USER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.EvidenceDocumentsMissingException;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;

@RestController
@Slf4j
public class EvidenceManagementController {

    private final EvidenceManagementService evidenceManagementService;
    private final EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    private final FileToPdfConversionService fileToPdfConversionService;
    private final boolean secureDocStoreEnabled;
    private final IdamService idamService;
    private final ObjectMapper objectMapper;
    private final AuthorisationService authorisationService;

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";


    @Autowired
    public EvidenceManagementController(EvidenceManagementService evidenceManagementService,
                                        EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService,
                                        FileToPdfConversionService fileToPdfConversionService,
                                        @Value("${feature.secure-doc-store.enabled:false}") boolean secureDocStoreEnabled,
                                        IdamService idamService,
                                        ObjectMapper objectMapper,
                                        AuthorisationService authorisationService) {
        this.evidenceManagementService = evidenceManagementService;
        this.evidenceManagementSecureDocStoreService = evidenceManagementSecureDocStoreService;
        this.fileToPdfConversionService = fileToPdfConversionService;
        this.secureDocStoreEnabled = secureDocStoreEnabled;
        this.idamService = idamService;
        this.objectMapper = objectMapper;
        this.authorisationService = authorisationService;
    }

    @Operation(summary = "Upload additional evidence converted to PDF",
        description = "Uploads evidence for an appeal which will be held in a draft state against the case that is not "
            + "visible by a caseworker in CCD. You will need to submit the evidence for it to be visbale in CCD "
            + "by a caseworker. You need to have an appeal in CCD."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evidence has been added to the appeal"),
    })
    @PostMapping(
        value = "/evidence/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> upload(
            @RequestHeader(value = SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestParam("file") List<MultipartFile> files
    ) throws JsonProcessingException {
        String serviceName = authorisationService.authenticate(serviceAuthorization);
        authorisationService.allowOnlySscs(serviceName);

        if (null == files || files.isEmpty()) {
            throw new EvidenceDocumentsMissingException();
        }

        try {
            List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(files);

            if (secureDocStoreEnabled) {
                List<Document> documents = evidenceManagementSecureDocStoreService
                        .upload(convertedFiles, idamService.getIdamTokens()).getDocuments();

                String jsonText =  "{\"documents\": " + objectMapper.writeValueAsString(documents) + "}";
                return ResponseEntity.ok(jsonText);
            } else {
                UploadResponse.Embedded embedded = evidenceManagementService
                        .upload(convertedFiles, DM_STORE_USER_ID)
                        .getEmbedded();
                List<uk.gov.hmcts.reform.document.domain.Document> documents = embedded.getDocuments();

                String jsonText =  "{\"documents\": " + objectMapper.writeValueAsString(documents) + "}";
                return ResponseEntity.ok(jsonText);
            }


        } catch (FileToPdfConversionException e) {
            log.error("Error while converting files for evidence upload: {}", e.getMessage());
            throw e;
        }
    }

}
