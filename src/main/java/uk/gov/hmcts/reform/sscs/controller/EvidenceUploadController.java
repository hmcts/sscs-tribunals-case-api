package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.unprocessableEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @ApiOperation(value = "Upload evidence",
            notes = "Uploads evidence for an appeal which will be held in a draft state against the case that is not "
                    + "visible by a caseworker in CCD. You will need to submit the evidence for it to be visbale in CCD "
                    + "by a caseworker. You need to have an appeal in CCD."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Evidence has been added to the appeal"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id"),
            @ApiResponse(code = 422, message = "The file cannot be added to the document store")
    })
    @RequestMapping(
            value = "{identifier}/evidence",
            method = RequestMethod.PUT,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Evidence> uploadEvidence(
            @ApiParam(value = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx") @PathVariable("identifier") String identifier,
            @RequestParam("file") MultipartFile file
    ) {
        return uploadEvidence(() -> evidenceUploadService.uploadDraftHearingEvidence(identifier, file));
    }

    @ApiOperation(value = "Upload COR evidence to a question",
            notes = "Uploads evidence for a COR appeal. You need to have an appeal in CCD and an online hearing in COH "
                    + "that references the appeal in CCD."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Evidence has been added to the appeal"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id"),
            @ApiResponse(code = 422, message = "The file cannot be added to the document store")
    })
    @RequestMapping(
            value = "{onlineHearingId}/questions/{questionId}/evidence",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Evidence> uploadEvidence(
            @PathVariable("onlineHearingId") String onlineHearingId,
            @PathVariable("questionId") String questionId,
            @RequestParam("file") MultipartFile file
    ) {
        return uploadEvidence(() -> evidenceUploadService.uploadDraftQuestionEvidence(onlineHearingId, questionId, file));
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

    @ApiOperation(value = "List evidence for a hearing",
            notes = "Lists the evidence that has already been uploaded for a hearing but is still in a draft state."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of draft evidence")
    })
    @RequestMapping(
            value = "{identifier}/evidence",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<Evidence>> listDraftEvidence(
            @PathVariable("identifier") String identifier
    ) {
        return ResponseEntity.ok(evidenceUploadService.listDraftHearingEvidence(identifier));
    }

    @ApiOperation(value = "Delete COR evidence",
            notes = "Deletes evidence for a COR appeal. You need to have an appeal in CCD and an online hearing in COH "
                    + "that references the appeal in CCD."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Evidence deleted "),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id"),
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(
            value = "{identifier}/evidence/{evidenceId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity deleteEvidence(
            @ApiParam(value = "either the online hearing or CCD case id", example = "xxxxx-xxxx-xxxx-xxxx")@PathVariable("identifier") String identifier,
            @PathVariable("evidenceId") String evidenceId
    ) {
        boolean hearingFound = evidenceUploadService.deleteDraftHearingEvidence(identifier, evidenceId);
        return hearingFound ? ResponseEntity.noContent().build() : notFound().build();
    }

    @ApiOperation(value = "Delete COR evidence from a question",
            notes = "Deletes evidence for a COR appeal. You need to have an appeal in CCD and an online hearing in COH "
                    + "that references the appeal in CCD."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Evidence deleted "),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id"),
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(
            value = "{onlineHearingId}/questions/{questionId}/evidence/{evidenceId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity deleteEvidence(
            @PathVariable("onlineHearingId") String onlineHearingId,
            @PathVariable("questionId") String questionId,
            @PathVariable("evidenceId") String evidenceId
    ) {
        boolean hearingFound = evidenceUploadService.deleteQuestionEvidence(onlineHearingId, evidenceId);
        return hearingFound ? ResponseEntity.noContent().build() : notFound().build();
    }

    @ApiOperation(value = "Submit COR evidence",
            notes = "Submits the evidence that has already been uploaded in a draft state. This means it will be "
                    + "visible in CCD by a caseworker and JUI by the panel members. You need to have an appeal in CCD "
                    + "and an online hearing in COH that references the appeal in CCD. Will create a cover sheet for the "
                    + "evidence uploaded containing the file names and a description from the appellant."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Evidence has been submitted to the appeal"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id")
    })
    @RequestMapping(
            value = "{identifier}/evidence",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity submitEvidence(
            @PathVariable("identifier") String identifier,
            @RequestBody EvidenceDescription description
    ) {
        boolean evidenceSubmitted = evidenceUploadService.submitHearingEvidence(identifier, description);
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
