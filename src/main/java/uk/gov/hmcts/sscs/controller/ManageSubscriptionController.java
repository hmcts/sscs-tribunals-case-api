package uk.gov.hmcts.sscs.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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

import uk.gov.hmcts.sscs.service.MessageAuthenticationService;

@RestController
public class ManageSubscriptionController {

    private final MessageAuthenticationService macService;

    @Autowired
    public ManageSubscriptionController(MessageAuthenticationService macService) {
        this.macService = macService;
    }

    @ApiOperation(value = "validateMacToken",
            notes = "Validates given mac token and returns Mac token details like subscription id, "
                    + "appeal id, decrypted token value, benefit type in the response json",
            response = String.class, responseContainer = "Mac token details")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Validated mac token", response = String.class)})
    @RequestMapping(value = "/appeals/tokens/{token}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validateMacToken(@PathVariable(value = "token") String macToken) {
        return ok().body(format("{\"benefitType\":\"%s\"}", macService.validateMacTokenAndReturnBenefitType(macToken)));
    }

}
