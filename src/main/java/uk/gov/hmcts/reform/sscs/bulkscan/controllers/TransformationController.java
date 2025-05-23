package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.slf4j.LoggerFactory.getLogger;

import javax.validation.Valid;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.SuccessfulTransformationResponse;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RestController
public class TransformationController {

    private static final Logger LOGGER = getLogger(TransformationController.class);

    private final AuthorisationService authService;
    private final CcdCallbackHandler handler;

    public TransformationController(
        AuthorisationService authService,
        CcdCallbackHandler handler
    ) {
        this.authService = authService;
        this.handler = handler;
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @PostMapping("/transform-exception-record")
    public SuccessfulTransformationResponse transform(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @Valid @RequestBody ExceptionRecord exceptionRecord
    ) {
        return getSuccessfulTransformationResponse(serviceAuthHeader, exceptionRecord);
    }

    @PostMapping("/transform-scanned-data")
    public SuccessfulTransformationResponse transformScannedData(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @Valid @RequestBody ExceptionRecord exceptionRecord
    ) {
        return getSuccessfulTransformationResponse(serviceAuthHeader, exceptionRecord);
    }

    private SuccessfulTransformationResponse getSuccessfulTransformationResponse(String serviceAuthHeader, ExceptionRecord exceptionRecord) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        LOGGER.info("Request received to transform from service {}", serviceName);

        authService.assertIsAllowedToHandleCallback(serviceName);

        return handler.handle(exceptionRecord);
    }
}
