package uk.gov.hmcts.reform.sscs.service.hearings;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.buildHearingPayload;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.getHearingId;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

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

    public void updateHearingAndCase(HearingRequest hearingRequest) throws ListingException, UpdateCaseException {
        HearingEvent event = HearingsServiceHelper.getHearingEvent(hearingRequest.getHearingState());
        try {
            updateCase(
                Long.valueOf(hearingRequest.getCcdCaseId()),
                event.getEventType().getCcdType(),
                idamService.getIdamTokens(),
                hearingRequest
            );
        } catch (FeignException ex) {
            UpdateCaseException exception = new UpdateCaseException(
                String.format("Failed to update case with Case id: %s could not be updated for hearing event %s, %s",
                              hearingRequest.getCcdCaseId(), event.getEventType().getCcdType(), ex
                ));
            log.error(exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    protected UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseDetails sscsCaseDetails, HearingRequest hearingRequest) throws ListingException {
        HearingEvent hearingEvent = updateHearing(createWrapper(hearingRequest, sscsCaseDetails));
        return new UpdateCcdCaseService.UpdateResult(hearingEvent.getSummary(), hearingEvent.getDescription());
    }

    private HearingEvent updateHearing(HearingWrapper wrapper) throws ListingException {
        if (isNull(wrapper.getCaseData().getSchedulingAndListingFields().getOverrideFields())) {
            OverridesMapping.setOverrideValues(wrapper.getCaseData(), referenceDataServiceHolder);
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

        log.debug(
            "Received Update Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
            wrapper.getCaseData().getCcdCaseId(),
            wrapper.getHearingState().getState(),
            response
        );

        return hearingResponseUpdate(wrapper, response);
    }


}
