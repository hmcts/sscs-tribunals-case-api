package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


@RestController
public class TyaController {

    private TribunalsService tribunalsService;

    private DocumentDownloadService documentDownloadService;

    @Autowired
    public TyaController(TribunalsService tribunalsService, DocumentDownloadService documentDownloadService) {
        this.tribunalsService = tribunalsService;
        this.documentDownloadService = documentDownloadService;
    }

    @ApiOperation(value = "getAppeal",
        notes = "Returns an appeal given the CCD case id",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAppealByCaseId(@RequestParam(value = "caseId") Long caseId,
            @RequestParam(value = "mya", required = false, defaultValue = "false") boolean mya) {
        return ok(tribunalsService.findAppeal(caseId, mya).toString());
    }

    @ApiOperation(value = "getDocument",
            notes = "Returns hearing outcome document given the document url",
            response = Resource.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Document", response = Resource.class)})
    @GetMapping(value = "/document", produces = APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getAppealDocument(@RequestParam(value = "url") String url) {
        return documentDownloadService.downloadFile(url);
    }
}
