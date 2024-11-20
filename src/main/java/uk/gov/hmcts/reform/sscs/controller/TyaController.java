package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


@RestController
public class TyaController {

    private TribunalsService tribunalsService;

    private DocumentDownloadService documentDownloadService;

    public TyaController(TribunalsService tribunalsService, DocumentDownloadService documentDownloadService) {
        this.tribunalsService = tribunalsService;
        this.documentDownloadService = documentDownloadService;
    }

    @Operation(summary = "getAppeal", description = "Returns an appeal given the CCD case id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Appeal", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @GetMapping(value = "/appeals", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAppealByCaseId(@RequestParam Long caseId,
            @RequestParam(required = false, defaultValue = "false") boolean mya) {
        return ok(tribunalsService.findAppeal(caseId, mya).toString());
    }

    @Operation(summary = "getDocument", description = "Returns hearing outcome document given the document url")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Document", content = {
        @Content(schema = @Schema(implementation = Resource.class))})})
    @GetMapping(value = "/document", produces = APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getAppealDocument(@RequestParam String url) {
        return documentDownloadService.downloadFile(url);
    }
}
