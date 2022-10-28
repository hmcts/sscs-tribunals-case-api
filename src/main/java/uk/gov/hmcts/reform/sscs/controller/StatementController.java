package uk.gov.hmcts.reform.sscs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Upload COR personal statement",
            description = "Uploads a personal statement for a COR appeal. You need to have an appeal in CCD. "
                    + "The statement is saved as a piece of evidence for the case in CCD as a PDF."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statement has been added to the appeal"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id")
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
