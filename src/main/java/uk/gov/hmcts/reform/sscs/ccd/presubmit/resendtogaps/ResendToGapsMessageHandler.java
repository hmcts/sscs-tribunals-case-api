package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    public void sendMessage(final String ccdCaseId) {
        if (gapsSwitchOverFeatureEnabled) {
            hearingMessagingServiceFactory
                .getMessagingService(LIST_ASSIST)
                .sendMessage(HearingRequest.builder(ccdCaseId)
                    .hearingRoute(LIST_ASSIST)
                    .hearingState(CANCEL_HEARING)
                    .build());
        }
    }
}
