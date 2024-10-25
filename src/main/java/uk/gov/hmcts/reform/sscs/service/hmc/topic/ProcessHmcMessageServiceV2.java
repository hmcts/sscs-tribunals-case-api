package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static java.util.Objects.isNull;

import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.DynamicEventUpdateResult;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.HearingUpdateException;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.helper.processing.ProcessHmcMessageHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessHmcMessageServiceV2 implements ProcessHmcMessageService {

    private final HmcHearingApiService hmcHearingApiService;
    private final HearingUpdateService hearingUpdateService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final ProcessHmcMessageHelper processHmcMessageHelper;
    private final CcdCaseService ccdCaseService;

    @Override
    public void processEventMessage(HmcMessage hmcMessage)
        throws CaseException, MessageProcessingException, InvalidMappingException {

        Long caseId = hmcMessage.getCaseId();
        String hearingId = hmcMessage.getHearingId();

        HearingGetResponse hearingResponse = hmcHearingApiService.getHearingRequest(hearingId);

        HmcStatus hmcMessageStatus = hmcMessage.getHearingUpdate().getHmcStatus();

        if (processHmcMessageHelper.stateNotHandled(hmcMessageStatus, hearingResponse)) {
            log.info("CCD state has not been updated using V2 for the Hearing ID {} and Case ID {}",
                     hearingId, caseId
            );
            return;
        }

        log.info("Processing message using V2 for HMC status {} with cancellation reasons {} for the Hearing ID {} and Case ID"
                     + " {}",
                 hmcMessageStatus, hearingResponse.getRequestDetails().getCancellationReasonCodes(),
                 hearingId, caseId
        );

        Function<SscsCaseDetails, DynamicEventUpdateResult> mutator = sscsCaseDetails -> {

            SscsCaseData sscsCaseData = sscsCaseDetails.getData();

            DwpState resolvedState = hearingUpdateService.resolveDwpState(hmcMessageStatus);
            if (resolvedState != null) {
                sscsCaseData.setDwpState(resolvedState);
            }
            if (processHmcMessageHelper.isHearingUpdated(hmcMessageStatus, hearingResponse)) {
                try {
                    hearingUpdateService.updateHearing(hearingResponse, sscsCaseData);
                } catch (InvalidMappingException | MessageProcessingException e) {
                    log.error("Error updating hearing using V2 for hearing ID {} and case ID {}", hearingId, caseId, e);
                    throw new HearingUpdateException(e.getMessage(), e);
                }
            }

            hearingUpdateService.setHearingStatus(hearingId, sscsCaseData, hmcMessageStatus);

            hearingUpdateService.setWorkBasketFields(hearingId, sscsCaseData, hmcMessageStatus);

            return resolveEventType(sscsCaseData, hmcMessageStatus, hearingResponse, hearingId);

        };

        IdamTokens idamTokens = idamService.getIdamTokens();

        updateCcdCaseService.updateCaseV2DynamicEvent(caseId, idamTokens, mutator);

        log.info(
            "Hearing message {} processed using V2 for case reference {}",
            hmcMessage.getHearingId(),
            hmcMessage.getCaseId()
        );
    }

    @Override
    public Boolean isProcessEventMessageV2Enabled() {
        return Boolean.TRUE;
    }

    private DynamicEventUpdateResult resolveEventType(SscsCaseData caseData, HmcStatus hmcMessageStatus, HearingGetResponse hearingResponse, String hearingId) {
        BiFunction<HearingGetResponse, SscsCaseData, EventType> eventMapper = hmcMessageStatus.getEventMapper();
        log.info("Using V2 PostponementRequest {}", caseData.getPostponement());

        DynamicEventUpdateResult noEventResult = new DynamicEventUpdateResult("", "", false, null);

        if (isNull(eventMapper)) {
            log.info("Case has not been updated using V2 for HMC Status {} with null eventMapper for the Case ID {}",
                     hmcMessageStatus, caseData.getCcdCaseId());
            return noEventResult;
        }

        EventType eventType = eventMapper.apply(hearingResponse, caseData);
        if (isNull(eventType)) {
            log.info("Case has not been updated using V2 for HMC Status {} with null eventType for the Case ID {}",
                     hmcMessageStatus, caseData.getCcdCaseId());
            return noEventResult;
        }

        String ccdUpdateDescription = String.format(hmcMessageStatus.getCcdUpdateDescription(), hearingId);
        return new DynamicEventUpdateResult(hmcMessageStatus.getCcdUpdateSummary(), ccdUpdateDescription, true, eventType.getCcdType());
    }
}
