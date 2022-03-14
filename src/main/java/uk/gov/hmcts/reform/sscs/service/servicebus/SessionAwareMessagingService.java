package uk.gov.hmcts.reform.sscs.service.servicebus;

import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

public interface SessionAwareMessagingService {

    boolean sendMessage(SessionAwareRequest message);
}
