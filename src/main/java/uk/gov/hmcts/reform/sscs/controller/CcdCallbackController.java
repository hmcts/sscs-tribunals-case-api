package uk.gov.hmcts.reform.sscs.controller;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RestController
@Slf4j
public class CcdCallbackController {

    private final AuthorisationService authorisationService;
    private final PreSubmitCallbackDispatcher<SscsCaseData> dispatcher;
    private final SscsCaseCallbackDeserializer deserializer;
    private final CcdCallbackHandler ccdCallbackHandler;

    @Autowired
    public CcdCallbackController(AuthorisationService authorisationService,
                                 SscsCaseCallbackDeserializer deserializer,
                                 PreSubmitCallbackDispatcher<SscsCaseData> dispatcher,
                                 CcdCallbackHandler ccdCallbackHandler) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
        this.ccdCallbackHandler = ccdCallbackHandler;
    }

    @PostMapping(path = "/ccdAboutToStart", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdAboutToStart(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {

        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start sscs case callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        return performRequest(ABOUT_TO_START, callback, userAuthorisation);
    }

    @PostMapping(path = "/ccdAboutToSubmit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdAboutToSubmit(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to submit sscs case callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());
        authorisationService.authorise(serviceAuthHeader);
        return performRequest(ABOUT_TO_SUBMIT, callback, userAuthorisation);
    }

    @PostMapping(path = "/ccdMidEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEvent(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestHeader(AUTHORIZATION) String userAuthorisation,
            @RequestBody String message,
            @RequestParam(value = "pageId", required = false, defaultValue = "") String pageId
    ) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        callback.setPageId(pageId);
        log.info("Midevent sscs case callback `{}` on page `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getPageId(), callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);
        return performRequest(MID_EVENT, callback, userAuthorisation);
    }

    @PostMapping(path = "/ccdSubmittedEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdSubmittedEvent(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        validateRequest(serviceAuthHeader, userAuthorisation, message);
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("Submitted event callback for`{}` event and Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());
        authorisationService.authorise(serviceAuthHeader);
        return performRequest(SUBMITTED, callback, userAuthorisation);
    }

    private void validateRequest(String serviceAuthHeader, String userAuthorisation, String message) {
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(userAuthorisation);
        Preconditions.checkNotNull(serviceAuthHeader);
    }

    private ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> performRequest(CallbackType callbackType,
                                                                                   Callback<SscsCaseData> callback,
                                                                                   String userAuthorisation) {
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = dispatcher.handle(callbackType, callback,
            userAuthorisation);

        log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        return ok(callbackResponse);
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

        log.info("Request received for to validate SSCS exception record id {}", callback.getCaseDetails().getId());

        String serviceName = authorisationService.authenticate(serviceAuthToken);

        log.info("Asserting that service {} is allowed to request validation of exception record {}", serviceName, callback.getCaseDetails().getId());

        authorisationService.assertIsAllowedToHandleCallback(serviceName);

        IdamTokens token = IdamTokens.builder().serviceAuthorization(serviceAuthToken).idamOauth2Token(userAuthToken).userId(userId).build();

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = ccdCallbackHandler.handleValidationAndUpdate(callback, token);

        return ResponseEntity.ok(ccdCallbackResponse);
    }
}
