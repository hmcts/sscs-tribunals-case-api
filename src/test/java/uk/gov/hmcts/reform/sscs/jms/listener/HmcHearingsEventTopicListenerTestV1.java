package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;


@ExtendWith(MockitoExtension.class)
class HmcHearingsEventTopicListenerTestV1 extends AbstractHmcHearingsEventTopicListenerTest {


    @Override
    public Boolean isProcessHmcMessageServiceV2Enabled() {
        return false;
    }

    @Override
    public void verifyProcessEventMessageCall(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2) throws CaseException, MessageProcessingException {
        verify(processHmcMessageService).processEventMessage((any(HmcMessage.class)));
    }

    @Override
    public void throwMessageProcessingException(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2, HmcMessage hmcMessage) throws MessageProcessingException, CaseException {
        doThrow(MessageProcessingException.class)
            .when(processHmcMessageService)
            .processEventMessage(hmcMessage);
    }
}
