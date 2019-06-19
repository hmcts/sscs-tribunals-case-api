package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RestController
@Slf4j
public class CcdCallbackController {

    private final AuthorisationService authorisationService;
    private final PreSubmitCallbackDispatcher dispatcher;
    private final SscsCaseCallbackDeserializer deserializer;

    @Autowired
    public CcdCallbackController(AuthorisationService authorisationService,
                                 SscsCaseCallbackDeserializer deserializer,
                                 PreSubmitCallbackDispatcher dispatcher) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
    }

    @PostMapping(path = "/ccdAboutToSubmit")
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdAboutToSubmit(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message
    ) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("Sscs Case CCD `{}` event `{}` received for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);
        return performRequest(callback);
    }

    private ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> performRequest(Callback<SscsCaseData> callback) {

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = dispatcher.handle(callback);

        log.info("Sscs Case CCD `{}` event `{}` handled for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());

        return ok(callbackResponse);
    }

}
