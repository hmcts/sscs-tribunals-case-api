package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.ResponseEntity.ok;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.validation.Valid;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.validation.OcrDataValidationRequest;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.validation.OcrValidationResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.validation.ValidationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RestController
public class OcrValidationController {
    private static final Logger logger = getLogger(OcrValidationController.class);

    private final CcdCallbackHandler handler;
    private final AuthorisationService authService;

    @Autowired
    public OcrValidationController(
        CcdCallbackHandler handler,
        AuthorisationService authService
    ) {
        this.handler = handler;
        this.authService = authService;
    }

    @PostMapping(
        path = "/forms/{form-type}/validate-ocr",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Validates OCR form data based on form type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation executed successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = OcrValidationResponse.class)) }),
        @ApiResponse(responseCode = "401", description = "Provided S2S token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "S2S token is not authorized to use the service")
    })
    public ResponseEntity<OcrValidationResponse> validateOcrData(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @PathVariable(name = "form-type", required = false) String formType,
        @Valid @RequestBody OcrDataValidationRequest ocrDataValidationRequest
    ) {
        String encodedFormType = UriUtils.encode(formType, StandardCharsets.UTF_8);
        if (!EnumUtils.isValidEnum(FormType.class, encodedFormType)) {
            logger.error("Invalid form type {} received when validating bulk scan", encodedFormType);

            return ok().body(new OcrValidationResponse(
                Collections.emptyList(),
                Collections.singletonList("Form type '" + encodedFormType + "' not found"),
                ValidationStatus.ERRORS
            ));
        }

        String serviceName = authService.authenticate(serviceAuthHeader);
        logger.info("Request received to validate ocr data from service {}", serviceName);

        authService.assertIsAllowedToHandleCallback(serviceName);

        CaseResponse result = handler.handleValidation(ExceptionRecord.builder().ocrDataFields(ocrDataValidationRequest.getOcrDataFields()).formType(formType).build());

        return ok().body(new OcrValidationResponse(result.getWarnings(), result.getErrors(), result.getStatus()));
    }

}
