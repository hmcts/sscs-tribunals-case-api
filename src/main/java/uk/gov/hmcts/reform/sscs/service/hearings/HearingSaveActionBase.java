package uk.gov.hmcts.reform.sscs.service.hearings;

import static java.util.Objects.isNull;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
public abstract class HearingSaveActionBase extends UpdateCcdCaseDetailsBase<HearingRequest> {

    protected final ReferenceDataServiceHolder referenceDataServiceHolder;

    protected HearingSaveActionBase(CcdClient ccdClient,
                                 SscsCcdConvertService sscsCcdConvertService, ReferenceDataServiceHolder referenceDataServiceHolder) {
        super(ccdClient, sscsCcdConvertService);
        this.referenceDataServiceHolder = referenceDataServiceHolder;
    }

    HearingWrapper createWrapper(HearingRequest hearingRequest, SscsCaseDetails sscsCaseDetails) {
        List<CancellationReason> cancellationReasons = null;

        if (hearingRequest.getCancellationReason() != null) {
            cancellationReasons = List.of(hearingRequest.getCancellationReason());
        }

        return HearingWrapper.builder()
            .caseData(sscsCaseDetails.getData())
            .eventId(sscsCaseDetails.getEventId())
            .eventToken(sscsCaseDetails.getEventToken())
            .caseState(State.getById(sscsCaseDetails.getState()))
            .hearingState(hearingRequest.getHearingState())
            .cancellationReasons(cancellationReasons)
            .build();
    }

    protected HearingEvent hearingResponseUpdate(HearingWrapper wrapper, HmcUpdateResponse response) {
        SscsCaseData caseData = wrapper.getCaseData();
        Long hearingRequestId = response.getHearingRequestId();
        String caseId = caseData.getCcdCaseId();

        log.info("Updating Case using updateCaseV3 with Hearing Response for Case ID {}, Hearing ID {} and Hearing State {}",
            caseId,
            hearingRequestId,
            wrapper.getHearingState().getState());

        HearingEvent event = HearingsServiceHelper.getHearingEvent(wrapper.getHearingState());
        log.info("Updating case using V3 {} with event {} description is {}", caseId, event, event.getDescription());

        updateCaseDataWithHearingResponse(response, hearingRequestId, wrapper.getCaseData());
        return event;
    }

    private void updateCaseDataWithHearingResponse(HmcUpdateResponse response, Long hearingRequestId, SscsCaseData caseData) {
        Hearing hearing = HearingsServiceHelper.getHearingById(hearingRequestId, caseData);

        if (isNull(hearing)) {
            hearing = HearingsServiceHelper.createHearing(hearingRequestId);
            HearingsServiceHelper.addHearing(hearing, caseData);
        }

        HearingsServiceHelper.updateHearingId(hearing, response);
        HearingsServiceHelper.updateVersionNumber(hearing, response);

        if (referenceDataServiceHolder.isAdjournmentFlagEnabled()
            && YesNo.isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
            log.debug("Updating case with AdjournmentInProgress to NO for Case ID {}", caseData.getCcdCaseId());
            caseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
    }
}
