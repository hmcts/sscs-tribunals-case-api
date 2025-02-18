package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;

@Component
/**
 * This factory provides the tribunals-to-hearings queue to publish messages (for LIST_ASSIST cases).
 * 
 * TODO: Replace this implementation with one that sends messages directly to TribunalsHearingsEventQueueListener. Then
 * we can also remove the JMS configuration and implementation.
 */
public class HearingMessagingServiceFactory {

    private final ServiceBusSenderClient hearingServiceBusClient;

    private final JmsTemplate jmsTemplate;

    @Value("${jms.tribunals-to-hearings-api.queue}")
    private String tribunalsToHearingsQueue;

    @Value("${jms.enabled}")
    private boolean jmsEnabled;

    public HearingMessagingServiceFactory(@Autowired(required = false) ServiceBusSenderClient hearingServiceBusClient,
                                          @Autowired(required = false) @Qualifier("hmcHearingsJmsTemplate") JmsTemplate jmsTemplate) {
        this.hearingServiceBusClient = hearingServiceBusClient;
        this.jmsTemplate = jmsTemplate;
    }

    public SessionAwareMessagingService getMessagingService(HearingRoute hearingRoute) {
        if (HearingRoute.LIST_ASSIST == hearingRoute) {
            if (jmsEnabled) {
                return new JmsMessagingService(jmsTemplate, tribunalsToHearingsQueue);
            } else {
                return new SessionAwareServiceBusMessagingService(hearingServiceBusClient);
            }
        }

        return new NoOpMessagingService();
    }
}
