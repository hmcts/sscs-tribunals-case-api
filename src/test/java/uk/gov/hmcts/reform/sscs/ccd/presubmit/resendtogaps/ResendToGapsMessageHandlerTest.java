package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareServiceBusMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;

@RunWith(JUnitParamsRunner.class)
public class ResendToGapsMessageHandlerTest {

    @Mock
    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Mock
    private SessionAwareServiceBusMessagingService sessionAwareServiceBusMessagingService;

    @Captor
    private ArgumentCaptor<HearingRequest> hearingRequestCaptor;

    private ResendToGapsMessageHandler handler;

    @Before
    public void setup() {
        openMocks(this);
        handler = new ResendToGapsMessageHandler(hearingMessagingServiceFactory);
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST)).thenReturn(sessionAwareServiceBusMessagingService);
    }

    @Test
    public void shouldSendExpectedMessage() {
        handler.sendMessage("1234");

        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());
        HearingRequest actualRequest = hearingRequestCaptor.getValue();

        assertEquals("1234", actualRequest.getCcdCaseId());
        assertEquals(LIST_ASSIST, actualRequest.getHearingRoute());
        assertEquals(CANCEL_HEARING, actualRequest.getHearingState());
    }

}