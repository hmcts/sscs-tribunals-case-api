package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.event.EventPublisher;


@RestController
@Slf4j
public class CcdCallbackOrchestratorController {
    private final EventPublisher eventPublisher;
    private final SscsCaseCallbackDeserializer mapper;

    public CcdCallbackOrchestratorController(final EventPublisher eventPublisher,
                                             final SscsCaseCallbackDeserializer mapper) {
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @RequestMapping(value = "/send", produces = APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestBody String body) {

        log.info("Received message: {}", body);
        Callback<SscsCaseData> callback = mapper.deserialize(body);
        log.info("Sending message for event: {} for case id: {}", callback.getEvent(), callback.getCaseDetails().getId());
        eventPublisher.publishEvent(callback);
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

}
