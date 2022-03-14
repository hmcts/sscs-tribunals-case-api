package uk.gov.hmcts.reform.sscs.service.servicebus;

import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

public class NoOpMessagingService implements SessionAwareMessagingService {

    @Override
    public boolean sendMessage(SessionAwareRequest message) {
        return true;
    }
}
