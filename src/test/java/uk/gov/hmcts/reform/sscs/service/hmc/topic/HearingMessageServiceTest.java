package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;

@ExtendWith(MockitoExtension.class)
public class HearingMessageServiceTest {

    @Mock
    private HearingMessageServiceListener hearingMessageServiceListener;

    private HearingRequest hearingRequest;

    private HearingMessageService hearingMessageService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        hearingMessageService = new HearingMessageService(hearingMessageServiceListener);

        hearingRequest = new HearingRequest();
        hearingRequest.setCcdCaseId("1234123412341234");
    }

    @Test
    public void shouldSendMessage() throws Exception {
        assertTrue(hearingMessageService.sendMessage(hearingRequest));
        verify(hearingMessageServiceListener, times(1)).handleIncomingMessage(hearingRequest);
    }

    @Test
    public void shouldNotSendMessage() throws Exception {
        doThrow(new TribunalsEventProcessingException("error")).when(hearingMessageServiceListener).handleIncomingMessage(any(HearingRequest.class));
        assertFalse(hearingMessageService.sendMessage(hearingRequest));
    }
}