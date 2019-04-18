package uk.gov.hmcts.reform.sscs.controller;

import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventService;

@RestController
@Slf4j
public class EventController {

    private final EventService eventService;
    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;

    @Autowired
    public EventController(EventService eventService,
                           AuthorisationService authorisationService,
                           SscsCaseCallbackDeserializer deserializer) {
        this.eventService = eventService;
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
    }

    @PostMapping(value = "/send", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    void sendEvent(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {

        Callback<SscsCaseData> callback = deserializer.deserialize(message);

        CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

        SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null, callback.getEvent());

        log.info("Event received for case id: {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        eventService.sendEvent(sscsCaseDataWrapper.getNotificationEventType(),
                sscsCaseDataWrapper.getNewSscsCaseData());

        log.info("Event handled for case {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, EventType event) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event).build();
    }

}
