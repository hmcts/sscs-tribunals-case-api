package uk.gov.hmcts.reform.sscs.service.servicebus.hearings;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;
import uk.gov.hmcts.reform.sscs.service.servicebus.JmsMessagingService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SessionAwareServiceBusMessagingService;


@ExtendWith(MockitoExtension.class)
public class HearingMessagingServiceFactoryTest {

    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @BeforeEach
    private void setUp() {
        hearingMessagingServiceFactory = new HearingMessagingServiceFactory(null, null);
    }

    @Test
    public void getMessagingService_HearingRouteListAssist()  {
        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);
        assertEquals(messagingService.getClass(), SessionAwareServiceBusMessagingService.class);
    }

    @Test
    public void getMessagingService_HearingRouteListAssist_Jms()  {
        ReflectionTestUtils.setField(hearingMessagingServiceFactory, "jmsEnabled", true);
        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);
        assertEquals(messagingService.getClass(), JmsMessagingService.class);
    }

    @Test
    public void getMessagingService_HearingRouteNotListAssist() {
        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.GAPS);
        assertEquals(messagingService.getClass(), NoOpMessagingService.class);
    }
}
