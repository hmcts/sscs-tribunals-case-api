package uk.gov.hmcts.sscs.tribunals.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.tribunals.service.CcdService;


@RestController
public class AppealsController {

    private CcdService ccdService;

    @Autowired
    public AppealsController(CcdService ccdService) {
        this.ccdService = ccdService;
    }

    @RequestMapping(value = "/appeals", method = POST,  consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createApppeals(@RequestBody String appealsJson) throws IOException {
        ccdService.saveCase(appealsJson);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
