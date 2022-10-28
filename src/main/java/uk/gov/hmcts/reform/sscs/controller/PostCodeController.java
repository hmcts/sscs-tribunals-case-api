package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

/**
 * Controller for handling post code queries.
 */
@RestController
@Slf4j
public class PostCodeController {

    @Autowired
    private AirLookupService airLookupService;

    @Operation(summary = "getRegionalCentre",
            description = "Returns the regional centre given the appellants full post code or out code")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Regional Centre", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @RequestMapping(value = "/regionalcentre/{postCode}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getRegionalCentre(@PathVariable(value = "postCode") String postCode) {
        String regionalCentre = airLookupService.lookupRegionalCentre(postCode);

        if (regionalCentre == null) {
            log.warn("Could not find postcode " + postCode);
            return notFound().build();
        }

        log.debug("Found regional centre " + regionalCentre + " for post code " + postCode);

        return ok("{\"regionalCentre\": \"" + regionalCentre + "\"}");
    }
}
