package uk.gov.hmcts.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.CcdService;

@RestController
public class AppealsController {

    private CcdService ccdService;

    @Autowired
    public AppealsController(CcdService ccdService) {
        this.ccdService = ccdService;
    }

    @ApiOperation(value = "submitAppeal",
        notes = "Creates an appeal from the SYA case details",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Submitted appeal successfully", response = String.class)})
    @RequestMapping(value = "/appeals", method = POST,  consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAppeals(@RequestBody SyaCaseWrapper syaCaseWrapper)
            throws CcdException {
        return status(ccdService.submitAppeal(syaCaseWrapper)).build();
    }

    @ApiOperation(value = "getAppeal",
        notes = "Checks valid appeal number and surname and then returns an appeal details",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}/surname/{surname}", method = GET,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getAppeal(@PathVariable(value = "appealNumber") String appealNumber,
                                            @PathVariable(value = "surname") String surname) {
        return ok(ccdService.generateResponse(appealNumber, surname).toString());
    }

    @ApiOperation(value = "getRootContext",
            notes = "Returns root context of tribunals case api",
            response = String.class, responseContainer = "Tribunals case api")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal api",
            response = String.class)})
    @RequestMapping(value="/", method = GET)
    public ResponseEntity<String> getRootContext(){
        return ResponseEntity.notFound().build();
    }
}
