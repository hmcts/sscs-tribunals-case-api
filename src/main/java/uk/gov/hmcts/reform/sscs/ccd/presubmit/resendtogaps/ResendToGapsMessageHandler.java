package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;

@Service
@RequiredArgsConstructor
public class ResendToGapsMessageHandler {

    private final HearingMessagingServiceFactory hearingMessagingServiceFactory;

    public boolean sendMessage(final String ccdCaseId) {
        return hearingMessagingServiceFactory
            .getMessagingService(LIST_ASSIST)
            .sendMessage(HearingRequest.builder(ccdCaseId)
                .hearingRoute(LIST_ASSIST)
                .hearingState(CANCEL_HEARING)
                .build());
    }
}
