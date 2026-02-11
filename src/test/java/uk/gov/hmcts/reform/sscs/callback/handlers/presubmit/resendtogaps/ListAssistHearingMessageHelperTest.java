package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.resendtogaps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;

@ExtendWith(MockitoExtension.class)
public class ListAssistHearingMessageHelperTest {

    @Mock
    private HearingRequestHandler hearingRequestHandler;

    @Captor
    private ArgumentCaptor<HearingRequest> hearingRequestCaptor;

    @InjectMocks
    private ListAssistHearingMessageHelper messageHelper;

    private static final String CCD_CASE_ID = "1234";

    @Test
    public void shouldSendExpectedCancellationMessage_sessionAwareMessageBus()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        messageHelper.sendListAssistCancelHearingMessage(CCD_CASE_ID, CancellationReason.OTHER);
        verify(hearingRequestHandler).handleHearingRequest(hearingRequestCaptor.capture());
        assertCancelHearingRequest(hearingRequestCaptor.getValue());
    }

    @Test
    public void shouldSendExpectedCreateMessage_sessionAwareMessageBus()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        messageHelper.sendListAssistCreateHearingMessage(CCD_CASE_ID);
        verify(hearingRequestHandler).handleHearingRequest(hearingRequestCaptor.capture());
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
