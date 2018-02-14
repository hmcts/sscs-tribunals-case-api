package uk.gov.hmcts.sscs.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TribunalsController {

    @ApiOperation(value = "getRootContext",
            notes = "Returns root context of tribunals case api",
            response = String.class, responseContainer = "Tribunals case api")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Appeal api",
            response = String.class)})
    @RequestMapping(value = "/", method = GET)
    public ResponseEntity<String> getRootContext() {
        return ResponseEntity.notFound().build();
    }
}
