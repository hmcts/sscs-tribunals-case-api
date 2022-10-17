package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;

@Component
public class HearingMessagingServiceFactory {

    private final ServiceBusSenderClient hearingServiceBusClient;

    private final JmsTemplate jmsTemplate;

    public HearingMessagingServiceFactory(@Autowired(required = false) ServiceBusSenderClient
                                              hearingServiceBusClient, @Autowired JmsTemplate jmsTemplate) {
        this.hearingServiceBusClient = hearingServiceBusClient;
        this.jmsTemplate = jmsTemplate;
    }

    public SessionAwareMessagingService getMessagingService(HearingRoute hearingRoute) {
        if (HearingRoute.LIST_ASSIST == hearingRoute) {
            return new SessionAwareServiceBusMessagingService(hearingServiceBusClient, jmsTemplate);
        }

        return new NoOpMessagingService();
    }
}
