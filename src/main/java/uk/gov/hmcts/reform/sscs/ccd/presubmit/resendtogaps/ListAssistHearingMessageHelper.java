package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.ADJOURN_CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessagingServiceFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListAssistHearingMessageHelper {

    private final HearingMessagingServiceFactory hearingMessagingServiceFactory;

    public void sendListAssistCancelHearingMessage(final String ccdCaseId, SscsCaseData caseData, State caseState, CancellationReason cancellationReason) {
        sendHearingMessage(ccdCaseId, caseData, caseState, LIST_ASSIST, CANCEL_HEARING, cancellationReason);
    }

    public void sendListAssistCreateAdjournmentHearingMessage(final String ccdCaseId, SscsCaseData caseData, State caseState) {
        sendHearingMessage(ccdCaseId, caseData, caseState, LIST_ASSIST, ADJOURN_CREATE_HEARING, null);
    }

    public void sendListAssistCreateHearingMessage(final String ccdCaseId, SscsCaseData caseData, State caseState) {
        sendHearingMessage(ccdCaseId, caseData, caseState, LIST_ASSIST, CREATE_HEARING, null);
    }

    public boolean sendHearingMessage(final String ccdCaseId, SscsCaseData caseData, State caseState,
        HearingRoute hearingRoute,
        HearingState hearingState,
        CancellationReason cancellationReason) {
        HearingRequest hearingRequest = HearingRequest.builder(ccdCaseId)
            .hearingRoute(hearingRoute)
            .hearingState(hearingState)
            .cancellationReason(cancellationReason)
            .build();
        return hearingMessagingServiceFactory
            .getMessagingService(hearingRoute)
            .sendMessage(hearingRequest, caseData, caseState);
    }
}
