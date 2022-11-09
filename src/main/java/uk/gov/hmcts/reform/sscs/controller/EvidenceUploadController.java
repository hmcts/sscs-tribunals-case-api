package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.unprocessableEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Evidence;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;
import uk.gov.hmcts.reform.sscs.service.evidence.EvidenceUploadService;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.IllegalFileTypeException;

@Slf4j
@RestController
@RequestMapping("/api/continuous-online-hearings")
public class EvidenceUploadController {
    private final EvidenceUploadService evidenceUploadService;
    private final CoversheetService coversheetService;

    @Autowired
    public EvidenceUploadController(
            EvidenceUploadService evidenceUploadService,
            CoversheetService coversheetService) {
        this.evidenceUploadService = evidenceUploadService;
        this.coversheetService = coversheetService;
    }

    @Operation(summary = "Upload evidence",
        description = "Uploads evidence for an appeal which will be held in a draft state against the case that is not "
            + "visible by a caseworker in CCD. You will need to submit the evidence for it to be visible in CCD "
            + "by a caseworker. You need to have an appeal in CCD."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evidence has been added to the appeal"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id"),
        @ApiResponse(responseCode = "422", description = "The file cannot be added to the document store")
    })
    @PutMapping(
        value = "{identifier}/evidence",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Evidence> uploadEvidence(
        @Parameter(description = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier,
        @RequestParam("file") MultipartFile file
    ) {
        return uploadEvidence(() -> evidenceUploadService.uploadDraftEvidence(identifier, file));
    }

    private ResponseEntity<Evidence> uploadEvidence(Supplier<Optional<Evidence>> uploadEvidence) {
        try {
            Optional<Evidence> evidenceOptional = uploadEvidence.get();
            return evidenceOptional.map(ResponseEntity::ok)
                    .orElse(notFound().build());
        } catch (IllegalFileTypeException exc) {
            log.info("Cannot upload file illegal file type", exc);
            return unprocessableEntity().build();
        }
    }

    @Operation(summary = "List evidence for a hearing",
        description = "Lists the evidence that has already been uploaded for a hearing but is still in a draft state."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of draft evidence")
    })
    @GetMapping(
        value = "{identifier}/evidence",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<Evidence>> listDraftEvidence(
        @PathVariable("identifier") String identifier
    ) {
        return ResponseEntity.ok(evidenceUploadService.listDraftHearingEvidence(identifier));
    }

    @Operation(summary = "Delete MYA evidence",
            description = "Deletes evidence for a MYA appeal. You need to have an appeal in CCD and an online hearing in MYA "
                    + "that references the appeal in CCD."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Evidence deleted "),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id"),
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @DeleteMapping(
        value = "{identifier}/evidence/{evidenceId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> deleteEvidence(
        @Parameter(description = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx")
        @PathVariable("identifier") String identifier,
        @PathVariable("evidenceId") String evidenceId
    ) {
        boolean hearingFound = evidenceUploadService.deleteDraftEvidence(identifier, evidenceId);
        return hearingFound ? ResponseEntity.noContent().build() : notFound().build();
    }

    @Operation(summary = "Submit MYA evidence",
        description = "Submits the evidence attached to the request. This means it will be "
            + "visible in CCD by a caseworker. You need to have an appeal in CCD "
            + "and an online hearing in the references the appeal in CCD. Will create a cover sheet for the "
            + "evidence uploaded containing the file names and a description from the appellant."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Evidence has been submitted to the appeal"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id"),
        @ApiResponse(responseCode = "422", description = "The file cannot be added to the document store")
    })
    @PostMapping(
            value = "{identifier}/singleevidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity submitSingleEvidence(
        @Parameter(description = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier,
        @RequestParam("file") MultipartFile file,
        @RequestParam("body") String body,
        @RequestParam("idamEmail") String idamEmail
    ) {
        boolean evidenceSubmitted = evidenceUploadService.submitSingleHearingEvidence(identifier, new EvidenceDescription(body, idamEmail), file);
        return evidenceSubmitted ? ResponseEntity.noContent().build() : notFound().build();
    }

    @Operation(summary = "Submit MYA evidence",
        description = "Submits the evidence that has already been uploaded in a draft state. This means it will be "
            + "visible in CCD by a caseworker. You need to have an appeal in CCD "
            + "and an online hearing in the references the appeal in CCD. Will create a cover sheet for the "
            + "evidence uploaded containing the file names and a description from the appellant."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Evidence has been submitted to the appeal"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id")
    })
    @PostMapping(
        value = "{identifier}/evidence",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity submitEvidence(
        @PathVariable("identifier") String identifier,
        @RequestBody EvidenceDescription description
    ) {
        boolean evidenceSubmitted = evidenceUploadService.submitHearingEvidence(identifier, description);
        return evidenceSubmitted ? ResponseEntity.noContent().build() : notFound().build();
    }

    @Operation(summary = "Get evidence cover sheet",
        description = "Generates a PDF file that can be printed out and added as a cover sheet to evidence that is to be "
            + "posted in. Can use either the CCD case id which is a number or online hearing id which is a GUUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A PDF cover sheet"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id")
    })
    @GetMapping(
        value = "{identifier}/evidence/coversheet",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<ByteArrayResource> getCoverSheet(
        @Parameter(description = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier
    ) {
        Optional<byte[]> coverSheet = coversheetService.createCoverSheet(identifier);
        return coverSheet.map(pdfBytes ->
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"evidence_cover_sheet.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdfBytes))
        ).orElse(ResponseEntity.notFound().build());
    }
}
