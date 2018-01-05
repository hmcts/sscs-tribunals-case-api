package uk.gov.hmcts.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RestController
public class AppealsController {

    private TribunalsService tribunalsService;

    @Autowired
    public AppealsController(TribunalsService tribunalsService) {
        this.tribunalsService = tribunalsService;
    }

    @RequestMapping(value = "/appeals", method = POST,  consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createApppeals(@RequestBody SyaCaseWrapper syaCaseWrapper)
            throws CcdException {
        return status(tribunalsService.submitAppeal(syaCaseWrapper)).build();
    }

}
