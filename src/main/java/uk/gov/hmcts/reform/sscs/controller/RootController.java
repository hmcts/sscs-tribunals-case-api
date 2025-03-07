package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.ok;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    /*
     * Azure hits us on / every 5 seconds to prevent it sleeping the application.
     * Application insights registers that as a 404 and adds it as an exception,
     * This is here to reduce the noise
     */
    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping(value = "/", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> welcome() {
        return ok("Welcome to sscs-tribunals-api");
    }
}


