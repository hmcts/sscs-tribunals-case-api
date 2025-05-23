package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;

@Component
@AllArgsConstructor
public class HearingMessagingServiceFactory {

    private final HearingMessageService hearingsMessageService;

    public SessionAwareMessagingService getMessagingService(HearingRoute hearingRoute) {
        if (HearingRoute.LIST_ASSIST == hearingRoute) {
            return hearingsMessageService;
        }

        return new NoOpMessagingService();
    }
}
