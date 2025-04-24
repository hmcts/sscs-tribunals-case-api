package uk.gov.hmcts.reform.sscs.service.servicebus;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareRequest;

public class NoOpMessagingService implements SessionAwareMessagingService {

    @Override
    public boolean sendMessage(SessionAwareRequest message, SscsCaseData sscsCaseData, State caseState) {
        return true;
    }
}
