package uk.gov.hmcts.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.service.AirLookupService;

/**
 * Controller for handling post code queries.
 */
@RestController
@Slf4j
public class PostCodeController {

    @Autowired
    private AirLookupService airLookupService;

    @ApiOperation(value = "getRegionalCentre",
            notes = "Returns the regional centre given the appellants full post code or out code",
            response = String.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Regional Centre", response = String.class)})
    @RequestMapping(value = "/regionalcentre/{postCode}", method = GET, produces = APPLICATION_JSON_UTF8_VALUE)
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
