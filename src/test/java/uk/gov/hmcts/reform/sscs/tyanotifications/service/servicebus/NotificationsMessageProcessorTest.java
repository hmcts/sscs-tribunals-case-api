package uk.gov.hmcts.reform.sscs.tyanotifications.service.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;
import uk.gov.hmcts.reform.sscs.tyanotifications.callback.handlers.FilterNotificationsEventsHandler;

public class NotificationsMessageProcessorTest {

    private static final String MESSAGE = "message";
    private static final Exception EXCEPTION = new RuntimeException("blah");

    @Mock
    private FilterNotificationsEventsHandler filterNotificationsEventsHandler;

    @Mock
    private SscsCaseCallbackDeserializer deserializer;

    private NotificationsMessageProcessor topicConsumer;
    private Exception exception;

    @Before
    public void setup() {
        openMocks(this);
        topicConsumer = new NotificationsMessageProcessor(deserializer, filterNotificationsEventsHandler);
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            123L,
            "jurisdiction",
            State.APPEAL_CREATED,
            SscsCaseData.builder().build(),
            LocalDateTime.now().minusMinutes(10),
            "Benefit-4106"
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED, true);
        when(deserializer.deserialize(any())).thenReturn(callback);
    }

    @Test
    public void anExceptionWillBeCaught() {
        exception = EXCEPTION;
        when(filterNotificationsEventsHandler.canHandle(any())).thenReturn(Boolean.TRUE);
        doThrow(exception).when(filterNotificationsEventsHandler).handle(any());
        topicConsumer.processMessage(MESSAGE, "1");
        verify(filterNotificationsEventsHandler, atLeastOnce()).handle(any());
    }


    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        when(filterNotificationsEventsHandler.canHandle(any())).thenReturn(Boolean.TRUE);
        doThrow(exception).when(filterNotificationsEventsHandler).handle(any());
        topicConsumer.processMessage(MESSAGE, "1");

        verify(filterNotificationsEventsHandler, atLeastOnce()).handle(any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        when(filterNotificationsEventsHandler.canHandle(any())).thenReturn(Boolean.TRUE);
        doThrow(exception).when(filterNotificationsEventsHandler).handle(any());
        topicConsumer.processMessage(MESSAGE, "1");
        verify(filterNotificationsEventsHandler, atLeastOnce()).handle(any());
    }

    @Test
    public void handleValidRequest() {
        when(filterNotificationsEventsHandler.canHandle(any())).thenReturn(Boolean.TRUE);
        topicConsumer.processMessage(MESSAGE, "1");
        verify(filterNotificationsEventsHandler).handle(any());
    }
}
