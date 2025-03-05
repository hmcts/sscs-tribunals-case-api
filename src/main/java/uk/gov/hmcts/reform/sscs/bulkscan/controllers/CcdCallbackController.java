package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.slf4j.LoggerFactory.getLogger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.auth.AuthService;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RestController
public class CcdCallbackController {

    private static final Logger logger = getLogger(CcdCallbackHandler.class);

    private final CcdCallbackHandler ccdCallbackHandler;

    private final AuthService authService;

    private final SscsCaseCallbackDeserializer deserializer;

    @Autowired
    public CcdCallbackController(
        CcdCallbackHandler ccdCallbackHandler,
        AuthService authService,
        SscsCaseCallbackDeserializer deserializer
    ) {
        this.ccdCallbackHandler = ccdCallbackHandler;
        this.authService = authService;
        this.deserializer = deserializer;
    }

    @PostMapping(path = "/validate-record",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Handles callback from SSCS to check case meets validation to change state to appeal created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Callback was processed successfully or in case of an error message is attached to the case",
            content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CallbackResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> handleValidationCallback(
        @RequestHeader(value = "Authorization") String userAuthToken,
        @RequestHeader(value = "serviceauthorization", required = false) String serviceAuthToken,
        @RequestHeader(value = "user-id") String userId,
        @RequestBody String message) {

        Callback<SscsCaseData> callback = deserializer.deserialize(message);

        logger.info("Request received for to validate SSCS exception record id {}", callback.getCaseDetails().getId());

        String serviceName = authService.authenticate(serviceAuthToken);

        logger.info("Asserting that service {} is allowed to request validation of exception record {}", serviceName, callback.getCaseDetails().getId());

        authService.assertIsAllowedToHandleCallback(serviceName);

        IdamTokens token = IdamTokens.builder().serviceAuthorization(serviceAuthToken).idamOauth2Token(userAuthToken).userId(userId).build();

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = ccdCallbackHandler.handleValidationAndUpdate(callback, token);

        return ResponseEntity.ok(ccdCallbackResponse);
    }
}
