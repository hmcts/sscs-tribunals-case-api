package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


@RestController
public class TyaController {

    private TribunalsService tribunalsService;

    @Autowired
    public TyaController(TribunalsService tribunalsService) {
        this.tribunalsService = tribunalsService;
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
}
