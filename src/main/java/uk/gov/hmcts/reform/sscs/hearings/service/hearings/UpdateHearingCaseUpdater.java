package uk.gov.hmcts.reform.sscs.hearings.service.hearings;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.hearings.helper.mapping.HearingsMapping.buildHearingPayload;
import static uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper.getHearingId;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.hearings.exception.ListingException;
import uk.gov.hmcts.reform.sscs.hearings.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.hearings.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.hearings.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.hearings.service.exceptions.UpdateCcdCaseDetailsException;
import uk.gov.hmcts.reform.sscs.hearings.service.holder.ReferenceDataServiceHolder;

@Component
@Slf4j
public class UpdateHearingCaseUpdater extends HearingSaveActionBase {

    private final HmcHearingApiService hmcHearingApiService;

    private final IdamService idamService;


    public UpdateHearingCaseUpdater(CcdClient ccdClient,
                                    SscsCcdConvertService sscsCcdConvertService, HmcHearingApiService hmcHearingApiService,
                                    ReferenceDataServiceHolder refData,
                                    IdamService idamService) {
        super(ccdClient, sscsCcdConvertService, refData);
        this.hmcHearingApiService = hmcHearingApiService;
        this.idamService = idamService;
    }

    public void updateHearingAndCase(HearingWrapper hearingWrapper) throws UpdateCcdCaseDetailsException {
        HearingEvent event = HearingsServiceHelper.getHearingEvent(hearingWrapper.getHearingState());
        updateCase(Long.valueOf(hearingWrapper.getCaseData().getCcdCaseId()), event.getEventType().getCcdType(), idamService.getIdamTokens(),
            hearingWrapper);
    }


    @Override
    protected UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseData data, HearingWrapper dto) throws UpdateCcdCaseDetailsException {
        try {
            updateHearing(dto);
            return new UpdateCcdCaseService.UpdateResult("Hearing updated", "Hearing updated");
        } catch (UpdateCaseException | ListingException e) {
            log.error("Failed to update case with hearing response for case id: {}", data.getCcdCaseId(), e);
            throw new UpdateCcdCaseDetailsException("Failed to update case with hearing response", e);
        }
    }

    private void updateHearing(HearingWrapper wrapper) throws UpdateCaseException, ListingException {
        if (isNull(wrapper.getCaseData().getSchedulingAndListingFields().getOverrideFields())) {
            OverridesMapping.setOverrideValues(wrapper, referenceDataServiceHolder);
        }
        Integer duration = wrapper
            .getCaseData()
            .getSchedulingAndListingFields()
            .getOverrideFields()
            .getDuration();
        boolean isMultipleOfFive = duration % 5 == 0;
        if (!isMultipleOfFive) {
            throw new ListingException("Listing duration must be multiple of 5.0 minutes");
        }

        HearingRequestPayload hearingPayload = buildHearingPayload(wrapper, referenceDataServiceHolder);
        String hearingId = getHearingId(wrapper);
        log.debug("Sending Update Hearing Request for Case ID {}", wrapper.getCaseData().getCcdCaseId());
        HmcUpdateResponse response = hmcHearingApiService.sendUpdateHearingRequest(hearingPayload, hearingId);

        log.debug("Received Update Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
            wrapper.getCaseData().getCcdCaseId(),
            wrapper.getHearingState().getState(),
            response);

        hearingResponseUpdate(wrapper, response);
    }


}
