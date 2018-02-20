package uk.gov.hmcts.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.sscs.domain.corecase.Subscription;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RestController
public class SubscriptionsController {

    private TribunalsService tribunalsService;

    @Autowired
    public SubscriptionsController(TribunalsService tribunalsService) {
        this.tribunalsService = tribunalsService;
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
