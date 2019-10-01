package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;
import uk.gov.hmcts.reform.sscs.service.exceptions.InvalidSurnameException;

@RestController
public class TyaController {

    private TribunalsService tribunalsService;

    @Autowired
    public TyaController(TribunalsService tribunalsService) {
        this.tribunalsService = tribunalsService;
    }

    @ApiOperation(value = "validateSurname",
        notes = "Checks valid appeal number and surname",
        response = ResponseEntity.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class),
        @ApiResponse(code = 404, message = "The surname could not be found")})
    @RequestMapping(value = "/appeals/{appealNumber}/surname/{surname}", method = GET,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SurnameResponse> validateSurname(
            @PathVariable(value = "appealNumber") String appealNumber,
            @PathVariable(value = "surname") String surname) {

        try {
            return ResponseEntity.ok(tribunalsService.validateSurname(appealNumber, surname));
        } catch (InvalidSurnameException invalidSurnameException) {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "getAppeal",
        notes = "Returns an appeal given the appeal number",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}", method = GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getAppeal(
            @PathVariable(value = "appealNumber") String appealNumber) {
        return ok(tribunalsService.findAppeal(appealNumber).toString());
    }

    @ApiOperation(value = "getAppeal",
            notes = "Returns an appeal given the CCD case id",
            response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals", method = GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getAppealByCaseId(
            @RequestParam(value = "caseId") Long caseId) {
        return ok(tribunalsService.findAppeal(caseId).toString());
    }
}
