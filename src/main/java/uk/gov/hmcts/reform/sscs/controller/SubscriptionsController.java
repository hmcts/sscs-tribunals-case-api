package uk.gov.hmcts.reform.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
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

    @Operation(summary = "validateMacToken",
        description = "Validates given mac token and returns Mac token details like subscription id, "
            + "appeal id, decrypted token value, benefit type in the response json")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Validated mac token", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @RequestMapping(value = "/tokens/{token}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validateMacToken(@PathVariable(value = "token") String macToken) throws JsonProcessingException {
        Map<String,Object> tokenDetails = macService.decryptMacToken(macToken);
        String json = new ObjectMapper().writeValueAsString(Collections.singletonMap("token", tokenDetails));
        return ok(json);
    }

    @Operation(summary = "UpdateSubscription")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Updated subscription", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @RequestMapping(value = "/appeals/{appealNumber}/subscriptions/{subscriptionId}",
            method = POST, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateSubscription(@RequestBody SubscriptionRequest subscriptionRequest,
                                                     @PathVariable String appealNumber,
                                                   @PathVariable String subscriptionId) {
        String benefitType = tribunalsService.updateSubscription(appealNumber, subscriptionRequest);
        return ok().body(format(BENEFIT_TYPE_FORMAT, benefitType));
    }

    @Operation(summary = "Unsubscribe email notification")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Removed email notification subscription", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @ResponseBody
    @RequestMapping(value = "/appeals/{id}/subscriptions/{subscriptionId}",
            method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable(value = "id") String appealNumber,
                                         @PathVariable String subscriptionId) {
        String benefitType = tribunalsService.unsubscribe(appealNumber);
        return ok().body(format(BENEFIT_TYPE_FORMAT, benefitType));
    }
}
