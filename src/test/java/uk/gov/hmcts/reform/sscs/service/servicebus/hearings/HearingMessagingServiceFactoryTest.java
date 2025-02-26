package uk.gov.hmcts.reform.sscs.service.servicebus.hearings;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessageService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessagingServiceFactory;


@ExtendWith(MockitoExtension.class)
public class HearingMessagingServiceFactoryTest {

    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Mock
    private HearingMessageService hearingsMessageService;

    @BeforeEach
    public void setUp() {
        hearingMessagingServiceFactory = new HearingMessagingServiceFactory(hearingsMessageService);
    }

    @Test
    public void getMessagingService_HearingRouteListAssist()  {
        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.LIST_ASSIST);
        assertEquals(messagingService.getClass(), HearingMessageService.class);
    }

    @Test
    public void getMessagingService_HearingRouteNotListAssist() {
        SessionAwareMessagingService messagingService = hearingMessagingServiceFactory
            .getMessagingService(HearingRoute.GAPS);
        assertEquals(messagingService.getClass(), NoOpMessagingService.class);
    }
}
