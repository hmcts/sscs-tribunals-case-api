package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import org.junit.Before;
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

    private static final String CCD_CASE_ID = "1234";

    @Before
    public void setUp() {
        when(hearingMessagingServiceFactory.getMessagingService(LIST_ASSIST))
            .thenReturn(sessionAwareServiceBusMessagingService);
    }

    @Test
    public void shouldSendExpectedCancellationMessage() {
        messageHelper.sendListAssistCancelHearingMessage(CCD_CASE_ID, CancellationReason.OTHER);
        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());

        HearingRequest actualRequest = hearingRequestCaptor.getValue();
        assertThat(actualRequest.getCcdCaseId()).isEqualTo(CCD_CASE_ID);
        assertThat(actualRequest.getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(actualRequest.getHearingState()).isEqualTo(CANCEL_HEARING);
        assertThat(actualRequest.getCancellationReason()).isEqualTo(CancellationReason.OTHER);
    }

    @Test
    public void shouldSendExpectedCreateMessage() {
        messageHelper.sendListAssistCreateHearingMessage(CCD_CASE_ID);
        verify(sessionAwareServiceBusMessagingService).sendMessage(hearingRequestCaptor.capture());

        HearingRequest actualRequest = hearingRequestCaptor.getValue();
        assertThat(actualRequest.getCcdCaseId()).isEqualTo(CCD_CASE_ID);
        assertThat(actualRequest.getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(actualRequest.getHearingState()).isEqualTo(CREATE_HEARING);
        assertThat(actualRequest.getCancellationReason()).isNull();
    }

}
