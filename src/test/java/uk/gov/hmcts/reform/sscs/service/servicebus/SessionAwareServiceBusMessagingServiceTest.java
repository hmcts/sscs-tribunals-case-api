package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.azure.spring.integration.core.AzureHeaders;
import com.azure.spring.integration.servicebus.converter.ServiceBusMessageHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

@RunWith(MockitoJUnitRunner.class)
public class SessionAwareServiceBusMessagingServiceTest {

    private SessionAwareServiceBusMessagingService sessionAwareServiceBusMessagingService;

    private static final String CASE_ID = "1234";

    @Mock
    private Sinks.Many<Message<SessionAwareRequest>> hearingsEventSink;

    @Before
    public void setup() {
        sessionAwareServiceBusMessagingService = new SessionAwareServiceBusMessagingService(hearingsEventSink);
    }

    @Test
    public void given_messageSuccessfullyEmittedShouldReturnTrue() {
        SessionAwareRequest request = HearingRequest.builder(CASE_ID)
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .hearingState(HearingState.HEARING_CREATED)
            .build();

        ArgumentCaptor<Message<SessionAwareRequest>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        boolean result = sessionAwareServiceBusMessagingService.sendMessage(request);

        verify(hearingsEventSink).emitNext(messageCaptor.capture(), any());

        Message<SessionAwareRequest> message = messageCaptor.getValue();

        assertThat(message.getPayload()).isEqualTo(request);
        assertThat(message.getHeaders()).contains(
            entry(ServiceBusMessageHeaders.SESSION_ID, CASE_ID),
            entry(AzureHeaders.PARTITION_KEY, CASE_ID));

        assertThat(result).isTrue();
    }

    @Test
    public void given_messageUnsuccessfullyEmittedShouldReturnFalse() {
        doThrow(Sinks.EmissionException.class).when(hearingsEventSink).emitNext(any(), any());

        SessionAwareRequest request = HearingRequest.builder(CASE_ID)
            .build();

        boolean result = sessionAwareServiceBusMessagingService.sendMessage(request);

        assertThat(result).isFalse();
    }
}
