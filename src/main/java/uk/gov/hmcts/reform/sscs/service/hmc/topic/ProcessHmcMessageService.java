package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;

public interface ProcessHmcMessageService {
    void processEventMessage(HmcMessage hmcMessage) throws CaseException, MessageProcessingException;

    Boolean isProcessEventMessageV2Enabled();
}
