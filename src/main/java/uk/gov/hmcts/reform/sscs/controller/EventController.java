package uk.gov.hmcts.reform.sscs.controller;

import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventService;
import uk.gov.hmcts.reform.sscs.service.InterlocService;

@RestController
@Slf4j
public class EventController {

    private final EventService eventService;
    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final InterlocService interlocService;
    private final SscsCaseDataSerializer sscsCaseDataSerializer;

    @Autowired
    public EventController(EventService eventService,
                           AuthorisationService authorisationService,
                           SscsCaseCallbackDeserializer deserializer,
                           InterlocService interlocService,
                           SscsCaseDataSerializer sscsCaseDataSerializer
    ) {
        this.eventService = eventService;
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.interlocService = interlocService;
        this.sscsCaseDataSerializer = sscsCaseDataSerializer;
    }

    @PostMapping(value = "/send", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public void sendEvent(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {

        SscsCaseDataWrapper sscsCaseDataWrapper = getSscsCaseDataWrapper(serviceAuthHeader, message);
        eventService.sendEvent(sscsCaseDataWrapper.getNotificationEventType(),
                sscsCaseDataWrapper.getNewSscsCaseData());

        log.info("Event handled for case {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());
    }

    @PostMapping(value = "/ccdAboutToSubmit", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<CallbackResponse> updateInterlocSecondaryState(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {

        SscsCaseDataWrapper sscsCaseDataWrapper = getSscsCaseDataWrapper(serviceAuthHeader, message);

        SscsCaseData sscsCaseData = interlocService.setInterlocSecondaryState(sscsCaseDataWrapper.getNotificationEventType(), sscsCaseDataWrapper.getNewSscsCaseData());

        log.info("Event handled for case {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        return ResponseEntity.ok(AboutToStartOrSubmitCallbackResponse.builder()
                .data(sscsCaseDataSerializer.serialize(sscsCaseData))
                .build()
        );
    }

    private SscsCaseDataWrapper getSscsCaseDataWrapper(String serviceAuthHeader, String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);

        CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

        SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null, callback.getEvent());

        log.info("Event received for case id: {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        return sscsCaseDataWrapper;
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, EventType event) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event).build();
    }
}
