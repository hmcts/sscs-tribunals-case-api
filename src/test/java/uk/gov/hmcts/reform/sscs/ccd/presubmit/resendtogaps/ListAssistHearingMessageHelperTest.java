package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;
import uk.gov.hmcts.reform.sscs.service.servicebus.JmsMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareServiceBusMessagingService;

public class ListAssistHearingMessageHelperTest {

    @Mock
    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Mock
    private SessionAwareServiceBusMessagingService sessionAwareServiceBusMessagingService;

    @Mock
    private JmsMessagingService jmsMessagingService;

    @Captor
    private ArgumentCaptor<HearingRequest> hearingRequestCaptor;

    @InjectMocks
    private ListAssistHearingMessageHelper messageHelper;

    private static final String CCD_CASE_ID = "1234";

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void shouldSendExpectedCancellationMessage_sessionAwareMessageBus() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST))
            .thenReturn(sessionAwareServiceBusMessagingService);
        messageHelper.sendListAssistCancelHearingMessage(CCD_CASE_ID, CancellationReason.OTHER);
        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());
        assertCancelHearingRequest(hearingRequestCaptor.getValue());
    }

    @Test
    public void shouldSendExpectedCreateMessage_sessionAwareMessageBus() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST))
            .thenReturn(sessionAwareServiceBusMessagingService);
        messageHelper.sendListAssistCreateHearingMessage(CCD_CASE_ID);
        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());
        assertCreateHearingRequest(hearingRequestCaptor.getValue());
    }

    @Test
    public void shouldSendExpectedCancellationMessage_jms() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST))
            .thenReturn(jmsMessagingService);
        messageHelper.sendListAssistCancelHearingMessage(CCD_CASE_ID, CancellationReason.OTHER);
        verify(jmsMessagingService).sendMessage(hearingRequestCaptor.capture());
        assertCancelHearingRequest(hearingRequestCaptor.getValue());

    }

    @Test
    public void shouldSendExpectedCreateMessage_jms() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST))
            .thenReturn(jmsMessagingService);
        messageHelper.sendListAssistCreateHearingMessage(CCD_CASE_ID);
        verify(jmsMessagingService).sendMessage(hearingRequestCaptor.capture());
        assertCreateHearingRequest(hearingRequestCaptor.getValue());
    }

    private void assertCreateHearingRequest(HearingRequest actualRequest) {
        assertThat(actualRequest.getCcdCaseId()).isEqualTo(CCD_CASE_ID);
        assertThat(actualRequest.getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(actualRequest.getHearingState()).isEqualTo(CREATE_HEARING);
        assertThat(actualRequest.getCancellationReason()).isNull();
    }

    private void assertCancelHearingRequest(HearingRequest actualRequest) {
        assertThat(actualRequest.getCcdCaseId()).isEqualTo(CCD_CASE_ID);
        assertThat(actualRequest.getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(actualRequest.getHearingState()).isEqualTo(CANCEL_HEARING);
        assertThat(actualRequest.getCancellationReason()).isEqualTo(CancellationReason.OTHER);
    }

}
