package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.notFound;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;
import uk.gov.hmcts.reform.sscs.service.evidence.EvidenceUploadService;

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

    @ApiOperation(value = "Submit MYA evidence",
            notes = "Submits the evidence that has already been uploaded in a draft state. This means it will be "
                    + "visible in CCD by a caseworker. You need to have an appeal in CCD "
                    + "and an online hearing in the references the appeal in CCD. Will create a cover sheet for the "
                    + "evidence uploaded containing the file names and a description from the appellant."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Evidence has been submitted to the appeal"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id"),
            @ApiResponse(code = 422, message = "The file cannot be added to the document store")
    })
    @PostMapping(
            value = "{identifier}/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity submitEvidence(
            @ApiParam(value = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier,
            @RequestParam("file") MultipartFile file,
            @RequestParam("body") String body,
            @RequestParam("idamEmail") String idamEmail
    ) {
        boolean evidenceSubmitted = evidenceUploadService.submitHearingEvidence(identifier, new EvidenceDescription(body, idamEmail), file);
        return evidenceSubmitted ? ResponseEntity.noContent().build() : notFound().build();
    }

    @ApiOperation(value = "Get evidence cover sheet",
            notes = "Generates a PDF file that can be printed out and added as a cover sheet to evidence that is to be "
                    + "posted in. Can use either the CCD case id which is a number or online hearing id which is a GUUID."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A PDF cover sheet"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id")
    })
    @GetMapping(
            value = "{identifier}/evidence/coversheet",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<ByteArrayResource> getCoverSheet(
            @ApiParam(value = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier
    ) {
        Optional<byte[]> coverSheet = coversheetService.createCoverSheet(identifier);
        return coverSheet.map(pdfBytes ->
                ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename="
                                + "\"evidence_cover_sheet.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new ByteArrayResource(pdfBytes))
        ).orElse(ResponseEntity.notFound().build());
    }
}
