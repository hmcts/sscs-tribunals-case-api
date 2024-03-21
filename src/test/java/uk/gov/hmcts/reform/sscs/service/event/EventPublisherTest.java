package uk.gov.hmcts.reform.sscs.service.event;

import static java.lang.Long.parseLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;

public class EventPublisherTest {

    private EventPublisher eventPublisher;

    @Mock
    private TopicPublisher topicPublisher;


    private SscsCaseData sscsCaseData;

    private static final String JURISDICTION = "Benefit";

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        eventPublisher = new EventPublisher(topicPublisher, mapper);
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .state(State.WITH_DWP)
            .appeal(Appeal.builder().build()).build();
    }

    @Test
    public void testPublishEvent() throws JsonProcessingException {

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION,
            sscsCaseData.getState(), sscsCaseData, LocalDateTime.now(), "Benefit");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(),
            EventType.DWP_SUPPLEMENTARY_RESPONSE, false);
        String message = mapper.writeValueAsString(callback);

        eventPublisher.publishEvent(callback);

        verify(topicPublisher).sendMessage(eq(message), eq(sscsCaseData.getCcdCaseId()), any(AtomicReference.class));
    }

}
