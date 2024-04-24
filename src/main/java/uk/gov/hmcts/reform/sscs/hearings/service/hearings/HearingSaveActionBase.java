package uk.gov.hmcts.reform.sscs.hearings.service.hearings;

import static java.util.Objects.isNull;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.hearings.service.holder.ReferenceDataServiceHolder;

@Slf4j
public abstract class HearingSaveActionBase extends UpdateCcdCaseDetailsBase<HearingWrapper> {

    protected final ReferenceDataServiceHolder referenceDataServiceHolder;
    public HearingSaveActionBase(CcdClient ccdClient,
                                 SscsCcdConvertService sscsCcdConvertService, ReferenceDataServiceHolder referenceDataServiceHolder) {
        super(ccdClient, sscsCcdConvertService);
        this.referenceDataServiceHolder = referenceDataServiceHolder;
    }

    public void hearingResponseUpdate(HearingWrapper wrapper, HmcUpdateResponse response) {
        SscsCaseData caseData = wrapper.getCaseData();
        Long hearingRequestId = response.getHearingRequestId();
        String caseId = caseData.getCcdCaseId();

        log.info("Updating Case with Hearing Response for Case ID {}, Hearing ID {} and Hearing State {}",
            caseId,
            hearingRequestId,
            wrapper.getHearingState().getState());

        HearingEvent event = HearingsServiceHelper.getHearingEvent(wrapper.getHearingState());
        log.info("Updating case with event {} description is {}", event, event.getDescription());

        updateCaseDataWithHearingResponse(response, hearingRequestId, wrapper.getCaseData());

        log.info("Case Updated with Hearing Response for Case ID {}, Hearing ID {}, Hearing State {} and CCD Event {}",
            caseId,
            hearingRequestId,
            wrapper.getHearingState().getState(),
            event.getEventType().getCcdType());
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
            log.debug("Case Updated with AdjournmentInProgress to NO for Case ID {}", caseData.getCcdCaseId());
            caseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
    }
}
