package uk.gov.hmcts.reform.sscs.controller;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.GetCaseCallbackService;

@RestController
@Slf4j
public class CcdGetCaseCallbackController {

    private final AuthorisationService authorisationService;
    private final GetCaseCallbackService getCaseCallbackService;

    @Autowired
    public CcdGetCaseCallbackController(final AuthorisationService authorisationService,
        final GetCaseCallbackService getCaseCallbackService) {
        this.authorisationService = authorisationService;
        this.getCaseCallbackService = getCaseCallbackService;
    }

    @PostMapping(path = "/ccdGetCase", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetCaseCallbackResponse> ccdGetCase(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) final String serviceAuthHeader,
        @RequestBody final Callback<SscsCaseData> callback) {

        checkNotNull(callback);
        checkNotNull(serviceAuthHeader);

        authorisationService.authorise(serviceAuthHeader);

        log.info("Submitted event callback for`{}` event and Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        return ok(getCaseCallbackService.buildResponse(callback));
    }
}
