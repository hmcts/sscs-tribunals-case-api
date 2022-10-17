package uk.gov.hmcts.reform.sscs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;
import uk.gov.hmcts.reform.sscs.service.AppellantStatementService;

@Slf4j
@RestController
@RequestMapping("/api/continuous-online-hearings")
public class StatementController {
    private final AppellantStatementService appellantStatementService;

    @Autowired
    public StatementController(AppellantStatementService appellantStatementService) {
        this.appellantStatementService = appellantStatementService;
    }

    @ApiOperation(value = "Upload COR personal statement",
            notes = "Uploads a personal statement for a COR appeal. You need to have an appeal in CCD. "
                    + "The statement is saved as a piece of evidence for the case in CCD as a PDF."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Statement has been added to the appeal"),
        @ApiResponse(code = 404, message = "No online hearing found with online hearing id")
    })
    @PostMapping(
            value = "/{identifier}/statement",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity uploadStatement(
        @PathVariable("identifier") String identifier,
        @RequestBody Statement statement) {
        log.info("upload statement for caseId {} and tya code {}", identifier, statement.getTya());
        return appellantStatementService
                .handleAppellantStatement(identifier, statement)
                .map(handled -> ResponseEntity.noContent().build())
                .orElse(ResponseEntity.notFound().build());
    }
}
