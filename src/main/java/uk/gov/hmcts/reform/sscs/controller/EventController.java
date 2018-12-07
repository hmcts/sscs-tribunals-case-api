package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.model.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventService;

@RestController
@Slf4j
public class EventController {

    private final EventService eventService;
    private final AuthorisationService authorisationService;

    @Autowired
    public EventController(EventService eventService, AuthorisationService authorisationService) {
        this.eventService = eventService;
        this.authorisationService = authorisationService;
    }

    @RequestMapping(value = "/events", method = POST, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    void submitEvent(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody SscsCaseDataWrapper sscsCaseDataWrapper) {

        log.info("Event received for case id: {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        eventService.submitEvent(sscsCaseDataWrapper.getNotificationEventType(),
                sscsCaseDataWrapper.getNewSscsCaseData());

        log.info("Event submitted for case {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());
    }

    @RequestMapping(value = "/actions", method = POST, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    void performAction(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody SscsCaseDataWrapper sscsCaseDataWrapper) {

        log.info("Action received for case id: {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        eventService.performAction(sscsCaseDataWrapper.getNotificationEventType(),
                sscsCaseDataWrapper.getNewSscsCaseData());

        log.info("Action performed for case {}, {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());
    }

}
