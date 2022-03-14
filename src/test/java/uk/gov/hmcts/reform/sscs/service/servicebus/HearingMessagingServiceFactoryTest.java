package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;

@RunWith(MockitoJUnitRunner.class)
public class HearingMessagingServiceFactoryTest {

    @InjectMocks
    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Test
    public void getMessagingService_HearingRouteListAssist() {

        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);

        assertThat(messagingService.getClass()).isEqualTo(SessionAwareServiceBusMessagingService.class);
    }

    @Test
    public void getMessagingService_HearingRouteNotListAssist() {

        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.GAPS);

        assertThat(messagingService.getClass()).isEqualTo(NoOpMessagingService.class);
    }
}
