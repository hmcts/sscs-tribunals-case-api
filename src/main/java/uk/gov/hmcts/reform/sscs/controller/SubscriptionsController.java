package uk.gov.hmcts.reform.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


@RestController
public class SubscriptionsController {

    public static final String BENEFIT_TYPE_FORMAT = "{\"benefitType\":\"%s\"}";
    private final MessageAuthenticationService macService;
    private TribunalsService tribunalsService;


    @Autowired
    public SubscriptionsController(MessageAuthenticationService macService, TribunalsService tribunalsService) {
        this.macService = macService;
        this.tribunalsService = tribunalsService;
    }

    @ApiOperation(value = "validateMacToken",
        notes = "Validates given mac token and returns Mac token details like subscription id, "
            + "appeal id, decrypted token value, benefit type in the response json",
        response = String.class, responseContainer = "Mac token details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Validated mac token", response = String.class)})
    @RequestMapping(value = "/tokens/{token}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validateMacToken(@PathVariable(value = "token") String macToken) throws JsonProcessingException {
        Map<String,Object> tokenDetails = macService.decryptMacToken(macToken);
        String json = new ObjectMapper().writeValueAsString(Collections.singletonMap("token", tokenDetails));
        return ok(json);
    }

    @ApiOperation(value = "UpdateSubscription",
            response = String.class, responseContainer = "benefit type for the updated subscription")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Updated subscription", response = String.class)})
    @RequestMapping(value = "/appeals/{appealNumber}/subscriptions/{subscriptionId}",
            method = POST, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateSubscription(@RequestBody SubscriptionRequest subscriptionRequest,
                                                     @PathVariable String appealNumber,
                                                   @PathVariable String subscriptionId) {
        String benefitType = tribunalsService.updateSubscription(appealNumber, subscriptionRequest);
        return ok().body(format(BENEFIT_TYPE_FORMAT, benefitType));
    }

    @ApiOperation(value = "Unsubscribe email notification",
            response = String.class, responseContainer = "unsubscribed appeal benefit type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Removed email notification "
            + "subscription", response = String.class)})
    @ResponseBody
    @RequestMapping(value = "/appeals/{id}/subscriptions/{subscriptionId}",
            method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable(value = "id") String appealNumber,
                                         @PathVariable String subscriptionId) {
        String benefitType = tribunalsService.unsubscribe(appealNumber);
        return ok().body(format(BENEFIT_TYPE_FORMAT, benefitType));
    }
}
