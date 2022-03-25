package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;

@Service
public class ResendToGapsMessageHandler {

    private final boolean gapsSwitchOverFeatureEnabled;
    private final HearingMessagingServiceFactory hearingMessagingServiceFactory;

    public ResendToGapsMessageHandler(@Value("${feature.gaps-switchover.enabled}") boolean gapsSwitchOverFeatureEnabled,
                                      @Autowired HearingMessagingServiceFactory hearingMessagingServiceFactory) {
        this.gapsSwitchOverFeatureEnabled = gapsSwitchOverFeatureEnabled;
        this.hearingMessagingServiceFactory = hearingMessagingServiceFactory;
    }

    public void sendMessage(final String ccdCaseId,
                            final HearingRoute hearingRoute,
                            final HearingState hearingState) {
        if (gapsSwitchOverFeatureEnabled) {
            hearingMessagingServiceFactory
                .getMessagingService(hearingRoute)
                .sendMessage(HearingRequest.builder(ccdCaseId)
                    .hearingRoute(hearingRoute)
                    .hearingState(hearingState)
                    .build());
        }
    }
}
