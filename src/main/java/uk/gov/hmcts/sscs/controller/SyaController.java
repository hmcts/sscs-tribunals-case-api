package uk.gov.hmcts.sscs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.status;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.service.SubmitAppealService;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SyaController {

    public static final String SYA_CASE_WRAPPER = "SyaCaseWrapper";
    public static final String UNDERSCORE = "_";

    private SubmitAppealService submitAppealService;

    @Autowired
    public SyaController(SubmitAppealService submitAppealService) {
        this.submitAppealService = submitAppealService;
    }

    @ApiOperation(value = "submitAppeal",
        notes = "Creates a case from the SYA details",
        response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Submitted appeal successfully",
            response = String.class)})
    @RequestMapping(value = "/appeals", method = POST,  consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAppeals(@RequestBody SyaCaseWrapper syaCaseWrapper) {
        String appellantLastName = syaCaseWrapper.getAppellant().getLastName();
        String nino = syaCaseWrapper.getAppellant().getNino();

        String appealUniqueIdentifier = appellantLastName + UNDERSCORE
                + nino.substring(nino.length() - 3);
        Map<String,Object> appealData = new HashMap<>();
        appealData.put(SYA_CASE_WRAPPER, syaCaseWrapper);

        submitAppealService.submitAppeal(syaCaseWrapper,appealUniqueIdentifier);

        return status(201).build();
    }
}
