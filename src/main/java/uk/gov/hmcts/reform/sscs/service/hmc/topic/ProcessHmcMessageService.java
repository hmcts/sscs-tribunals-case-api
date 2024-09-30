package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.CANCELLED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.EXCEPTION;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.LISTED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.UPDATE_SUBMITTED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus.CNCL;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus.FIXED;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessHmcMessageService {

    private final HmcHearingApiService hmcHearingApiService;

    private final CcdCaseService ccdCaseService;

    private final HearingUpdateService hearingUpdateService;

    public void processEventMessage(HmcMessage hmcMessage)
            throws CaseException, MessageProcessingException {

        Long caseId = hmcMessage.getCaseId();
        String hearingId = hmcMessage.getHearingId();

        HearingGetResponse hearingResponse = hmcHearingApiService.getHearingRequest(hearingId);

        HmcStatus hmcMessageStatus = hmcMessage.getHearingUpdate().getHmcStatus();

        if (stateNotHandled(hmcMessageStatus, hearingResponse)) {
            log.info("CCD state has not been updated for the Hearing ID {} and Case ID {}",
                    hearingId, caseId
            );
            return;
        }

        log.info("Processing message for HMC status {} with cancellation reasons {} for the Hearing ID {} and Case ID"
                        + " {}",
                hmcMessageStatus, hearingResponse.getRequestDetails().getCancellationReasonCodes(),
                hearingId, caseId
        );

        SscsCaseData caseData = ccdCaseService.getCaseDetails(caseId).getData();

        DwpState resolvedState = hearingUpdateService.resolveDwpState(hmcMessageStatus);
        if (resolvedState != null) {
            caseData.setDwpState(resolvedState);
        }
        if (isHearingUpdated(hmcMessageStatus, hearingResponse)) {
            hearingUpdateService.updateHearing(hearingResponse, caseData);
        }

        hearingUpdateService.setHearingStatus(hearingId, caseData, hmcMessageStatus);
        hearingUpdateService.setWorkBasketFields(hearingId, caseData, hmcMessageStatus);

        String ccdUpdateDescription = String.format(hmcMessageStatus.getCcdUpdateDescription(), hearingId);

        resolveEventAndUpdateCase(hearingResponse, hmcMessageStatus, caseData, ccdUpdateDescription);

        log.info(
                "Hearing message {} processed for case reference {}",
                hmcMessage.getHearingId(),
                hmcMessage.getCaseId()
        );
    }

    private void resolveEventAndUpdateCase(HearingGetResponse hearingResponse, HmcStatus hmcStatus, SscsCaseData caseData,
                                           String ccdUpdateDescription) throws UpdateCaseException {

        BiFunction<HearingGetResponse, SscsCaseData, EventType> eventMapper = hmcStatus.getEventMapper();
        log.info("PostponementRequest {}", caseData.getPostponement());

        if (isNull(eventMapper)) {
            log.info("Case has not been updated for HMC Status {} with null eventMapper for the Case ID {}",
                    hmcStatus, caseData.getCcdCaseId());
            return;
        }

        EventType eventType = eventMapper.apply(hearingResponse, caseData);
        if (isNull(eventType)) {
            log.info("Case has not been updated for HMC Status {} with null eventType for the Case ID {}",
                    hmcStatus, caseData.getCcdCaseId());
            return;
        }

        ccdCaseService.updateCaseData(
                caseData,
                eventType,
                hmcStatus.getCcdUpdateSummary(),
                ccdUpdateDescription);
    }

    private boolean stateNotHandled(HmcStatus hmcStatus, HearingGetResponse hearingResponse) {
        return !(isHearingUpdated(hmcStatus, hearingResponse) || isHearingCancelled(hmcStatus, hearingResponse)
                || isStatusException(hmcStatus));
    }

    private boolean isHearingUpdated(HmcStatus hmcStatus, HearingGetResponse hearingResponse) {
        return isHearingListedOrUpdateSubmitted(hmcStatus)
                && isStatusFixed(hearingResponse);
    }

    private boolean isHearingListedOrUpdateSubmitted(HmcStatus hmcStatus) {
        return hmcStatus == LISTED || hmcStatus == AWAITING_LISTING || hmcStatus == UPDATE_SUBMITTED;
    }

    private boolean isStatusFixed(HearingGetResponse hearingResponse) {
        return FIXED == hearingResponse.getHearingResponse().getListingStatus();
    }

    private boolean isHearingCancelled(HmcStatus hmcStatus, HearingGetResponse hearingResponse) {
        return hmcStatus == CANCELLED
                || isNotEmpty(hearingResponse.getRequestDetails().getCancellationReasonCodes())
                || CNCL == hearingResponse.getHearingResponse().getListingStatus();
    }

    private boolean isStatusException(HmcStatus hmcStatus) {
        return hmcStatus == EXCEPTION;
    }
}
