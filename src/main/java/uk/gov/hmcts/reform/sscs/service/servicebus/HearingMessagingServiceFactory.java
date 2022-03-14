package uk.gov.hmcts.reform.sscs.service.servicebus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

@Component
public class HearingMessagingServiceFactory {

    @Autowired(required = false)
    private Sinks.Many<Message<SessionAwareRequest>> eventSink;


    public SessionAwareMessagingService getMessagingService(HearingRoute hearingRoute) {
        if (HearingRoute.LIST_ASSIST == hearingRoute) {
            return new SessionAwareServiceBusMessagingService(eventSink);
        }

        return new NoOpMessagingService();
    }
}
