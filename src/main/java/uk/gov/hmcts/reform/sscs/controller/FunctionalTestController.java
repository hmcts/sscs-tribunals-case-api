package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.servicebus.SendCallbackHandler;

@RestController
@Slf4j
@ConditionalOnProperty(prefix = "testing-support", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FunctionalTestController {

    private final SendCallbackHandler sendCallbackHandler;
    private final SscsCaseCallbackDeserializer mapper;

    public FunctionalTestController(SendCallbackHandler sendCallbackHandler, SscsCaseCallbackDeserializer mapper) {
        this.sendCallbackHandler = sendCallbackHandler;
        this.mapper = mapper;
    }

    @PostMapping(value = "/testing-support/send", produces = APPLICATION_JSON_VALUE)
    public void send(@RequestBody String message) {

        Callback<SscsCaseData> callback = mapper.deserialize(message);
        log.info("Sending message for event: {} for case id: {}", callback.getEvent(), callback.getCaseDetails().getId());

        sendCallbackHandler.handle(callback);
    }
}
