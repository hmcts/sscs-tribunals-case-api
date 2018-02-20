package uk.gov.hmcts.sscs.controller;

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
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RestController
public class TyaController {

    private TribunalsService tribunalsService;

    @Autowired
    public TyaController(TribunalsService tribunalsService) {
        this.tribunalsService = tribunalsService;
    }

    @ApiOperation(value = "validateSurname",
        notes = "Checks valid appeal number and surname",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}/surname/{surname}", method = GET,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public void validateSurname(
            @PathVariable(value = "appealNumber") String appealNumber,
            @PathVariable(value = "surname") String surname) throws CcdException {
        tribunalsService.validateSurname(appealNumber, surname);
    }

    @ApiOperation(value = "getAppeal",
        notes = "Returns an appeal given the appeal number",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}", method = GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getAppeal(
            @PathVariable(value = "appealNumber") String appealNumber) throws CcdException {
        return ok(tribunalsService.findAppeal(appealNumber).toString());
    }
}
