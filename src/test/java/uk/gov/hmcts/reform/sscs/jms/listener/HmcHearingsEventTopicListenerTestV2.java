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
class HmcHearingsEventTopicListenerTestV2 extends AbstractHmcHearingsEventTopicListenerTest {


    @Override
    public Boolean isProcessHmcMessageServiceV2Enabled() {
        return true;
    }

    @Override
    public void verifyProcessEventMessageCall(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2) throws CaseException, MessageProcessingException {
        verify(processHmcMessageServiceV2).processEventMessageV2((any(HmcMessage.class)));
    }

    @Override
    public void throwMessageProcessingException(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2, HmcMessage hmcMessage) throws MessageProcessingException, CaseException {
        doThrow(MessageProcessingException.class)
            .when(processHmcMessageServiceV2)
            .processEventMessageV2(hmcMessage);
    }
}
