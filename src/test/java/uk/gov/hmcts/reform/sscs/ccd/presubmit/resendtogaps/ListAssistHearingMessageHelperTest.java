package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareServiceBusMessagingService;

@RunWith(MockitoJUnitRunner.class)
public class ListAssistHearingMessageHelperTest {

    @Mock
    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Mock
    private SessionAwareServiceBusMessagingService sessionAwareServiceBusMessagingService;

    @Captor
    private ArgumentCaptor<HearingRequest> hearingRequestCaptor;

    @InjectMocks
    private ListAssistHearingMessageHelper messageHelper;

    @Test
    public void shouldSendExpectedCancellationMessage() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST)).thenReturn(sessionAwareServiceBusMessagingService);

        messageHelper.sendListAssistCancelHearingMessage("1234");

        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());

        HearingRequest actualRequest = hearingRequestCaptor.getValue();
        assertEquals("1234", actualRequest.getCcdCaseId());
        assertEquals(LIST_ASSIST, actualRequest.getHearingRoute());
        assertEquals(CANCEL_HEARING, actualRequest.getHearingState());
        assertThat(actualRequest.getCancellationReason(), is(CancellationReason.OTHER));
    }

}
