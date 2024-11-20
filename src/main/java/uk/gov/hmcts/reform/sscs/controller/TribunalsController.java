package uk.gov.hmcts.reform.sscs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TribunalsController {

    @Operation(summary = "getRootContext", description = "Returns root context of tribunals case api")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Appeal api", content = {
        @Content(schema = @Schema(implementation = String.class))})})
    @GetMapping("/")
    public ResponseEntity<String> getRootContext() {
        return ResponseEntity.notFound().build();
    }
}
