package uk.gov.hmcts.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.SubmitAppealService;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RestController
public class AppealsController {

    public static final String SYA_CASE_WRAPPER = "SyaCaseWrapper";
    public static final String UNDERSCORE = "_";

    private SubmitAppealService submitAppealService;
    private TribunalsService tribunalsService;

    @Autowired
    public AppealsController(TribunalsService tribunalsService, SubmitAppealService submitAppealService) {
        this.tribunalsService = tribunalsService;
        this.submitAppealService = submitAppealService;
    }

    @ApiOperation(value = "submitAppeal",
        notes = "Creates a case from the SYA details",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Submitted appeal successfully",
            response = String.class)})
    @RequestMapping(value = "/appeals", method = POST,  consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAppeals(@RequestBody SyaCaseWrapper syaCaseWrapper)
            throws CcdException {
        String appellantLastName = syaCaseWrapper.getAppellant().getLastName();
        String nino = syaCaseWrapper.getAppellant().getNino();

        String appealUniqueIdentifier = appellantLastName + UNDERSCORE
                + nino.substring(nino.length() - 3);
        Map<String,Object> appealData = new HashMap<>();
        appealData.put(SYA_CASE_WRAPPER, syaCaseWrapper);
        submitAppealService.submitAppeal(appealData,appealUniqueIdentifier);
        return status(tribunalsService.submitAppeal(syaCaseWrapper)).build();
    }

    @ApiOperation(value = "getAppeal",
        notes = "Checks valid appeal number and surname and then returns an appeal details",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}/surname/{surname}", method = GET,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getAppeal(
            @PathVariable(value = "appealNumber") String appealNumber,
            @PathVariable(value = "surname") String surname) throws CcdException {
        return ok(tribunalsService.findAppeal(appealNumber).toString());
    }

    @ApiOperation(value = "unsubscribe", notes = "Removes subscription and returns benefit type in the response json",
            response = String.class, responseContainer = "Unsubscribed appeal benefit type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Removed subscription", response = String.class)})
    @ResponseBody
    @RequestMapping(value = "/appeals/{appealNumber}/subscribe", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable String appealNumber) throws CcdException {
        String benefitType = tribunalsService.unsubscribe(appealNumber);
        return ok().body(format("{\"benefitType\":\"%s\"}", benefitType));
    }

    @ApiOperation(value = "getRootContext",
            notes = "Returns root context of tribunals case api",
            response = String.class, responseContainer = "Tribunals case api")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal api",
            response = String.class)})
    @RequestMapping(value = "/", method = GET)
    public ResponseEntity<String> getRootContext() {
        return ResponseEntity.notFound().build();
    }
}
