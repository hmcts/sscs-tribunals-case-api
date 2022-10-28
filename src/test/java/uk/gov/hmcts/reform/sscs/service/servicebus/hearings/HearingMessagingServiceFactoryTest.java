package uk.gov.hmcts.reform.sscs.service.servicebus.hearings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;
import uk.gov.hmcts.reform.sscs.service.servicebus.JMSMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareServiceBusMessagingService;


@RunWith(MockitoJUnitRunner.class)
public class HearingMessagingServiceFactoryTest {

    @Test
    public void getMessagingService_HearingRouteListAssist()  {
        HearingMessagingServiceFactory hearingMessagingServiceFactory = new HearingMessagingServiceFactory(null, null);

        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);

        assertThat(messagingService.getClass()).isEqualTo(SessionAwareServiceBusMessagingService.class);
    }

    @Test
    public void getMessagingService_HearingRouteListAssist_JMS()  {
        HearingMessagingServiceFactory hearingMessagingServiceFactory = new HearingMessagingServiceFactory(null, null);
        ReflectionTestUtils.setField(hearingMessagingServiceFactory, "jmsEnabled", true);

        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);

        assertThat(messagingService.getClass()).isEqualTo(JMSMessagingService.class);
    }

    @Test
    public void getMessagingService_HearingRouteNotListAssist() {
        HearingMessagingServiceFactory hearingMessagingServiceFactory = new HearingMessagingServiceFactory(null, null);

        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.GAPS);

        assertThat(messagingService.getClass()).isEqualTo(NoOpMessagingService.class);
    }
}
