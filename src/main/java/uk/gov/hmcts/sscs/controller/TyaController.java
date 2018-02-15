package uk.gov.hmcts.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.sscs.domain.corecase.Subscription;
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

    @ApiOperation(value = "unsubscribe", notes = "Removes subscription and returns benefit type in the response json",
            response = String.class, responseContainer = "Unsubscribed appeal benefit type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Removed subscription", response = String.class)})
    @ResponseBody
    @RequestMapping(value = "/appeals/{appealNumber}/subscribe/reason/{reason}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable String appealNumber, @PathVariable String reason)
            throws CcdException {
        String benefitType = tribunalsService.unsubscribe(appealNumber, reason);
        return ok().body(format("{\"benefitType\":\"%s\"}", benefitType));
    }

    @ApiOperation(value = "updateSubscription", notes = "Updates subscription and returns benefit type in the response json",
            response = String.class, responseContainer = "Updated appeal benefit type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Removed subscription", response = String.class)})
    @ResponseBody
    @RequestMapping(value = "/appeals/{appealNumber}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateSubscription(@PathVariable String appealNumber, @RequestBody Subscription subscription)
            throws CcdException {
        String benefitType = tribunalsService.updateSubscription(appealNumber, subscription);
        return ok().body(format("{\"benefitType\":\"%s\"}", benefitType));
    }

}
