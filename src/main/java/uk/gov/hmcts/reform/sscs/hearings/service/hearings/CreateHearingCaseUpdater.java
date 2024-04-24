package uk.gov.hmcts.reform.sscs.hearings.service.hearings;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.hearings.helper.mapping.HearingsMapping.buildHearingPayload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.hearings.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.hearings.exception.ListingException;
import uk.gov.hmcts.reform.sscs.hearings.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.hearings.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.hearings.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.hearings.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.hearings.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.hearings.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.hearings.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.hearings.service.exceptions.UpdateCcdCaseDetailsException;
import uk.gov.hmcts.reform.sscs.hearings.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Component
public class CreateHearingCaseUpdater extends HearingSaveActionBase {

    private final HmcHearingApiService hmcHearingApiService;

    private final HmcHearingsApiService hmcHearingsApiService;

    private final CcdCaseService ccdCaseService;

    private final ReferenceDataServiceHolder refData;

    private final IdamService idamService;

    private static final Long HEARING_VERSION_NUMBER = 1L;


    @Autowired
    public CreateHearingCaseUpdater(CcdClient ccdClient, SscsCcdConvertService sscsCcdConvertService, HmcHearingApiService hmcHearingApiService,
                                    HmcHearingsApiService hmcHearingsApiService, CcdCaseService ccdCaseService, ReferenceDataServiceHolder refData,
                                    IdamService idamService) {
        super(ccdClient, sscsCcdConvertService, refData);
        this.hmcHearingApiService = hmcHearingApiService;
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.ccdCaseService = ccdCaseService;
        this.refData = refData;
        this.idamService = idamService;
    }


    public void createHearingAndUpdateCase(HearingWrapper hearingWrapper) throws UpdateCcdCaseDetailsException {
        HearingEvent event = HearingsServiceHelper.getHearingEvent(hearingWrapper.getHearingState());
        updateCase(Long.valueOf(hearingWrapper.getCaseData().getCcdCaseId()), event.getEventType().getCcdType(), idamService.getIdamTokens(),
            hearingWrapper);
    }

    @Override
    protected UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseData data, HearingWrapper hearingWrapper) throws UpdateCcdCaseDetailsException {
        try {
            createHearing(hearingWrapper);
            return new UpdateCcdCaseService.UpdateResult("Hearing created", "Hearing created");
        } catch (UpdateCaseException | ListingException e) {
            log.error("Failed to update case with hearing response for case id: {}", data.getCcdCaseId(), e);
            throw new UpdateCcdCaseDetailsException("Failed to update case with hearing response", e);
        }
    }


    private void createHearing(HearingWrapper wrapper) throws UpdateCaseException, ListingException {
        SscsCaseData caseData = wrapper.getCaseData();

        String caseId = caseData.getCcdCaseId();
        HearingsGetResponse hearingsGetResponse = hmcHearingsApiService.getHearingsRequest(caseId, null);
        CaseHearing hearing = HearingsServiceHelper.findExistingRequestedHearings(hearingsGetResponse);
        HmcUpdateResponse hmcUpdateResponse;

        OverridesMapping.setDefaultListingValues(wrapper, refData);

        if (isNull(hearing)) {
            HearingRequestPayload hearingPayload = buildHearingPayload(wrapper, refData);
            log.debug("Sending Create Hearing Request for Case ID {}", caseId);
            hmcUpdateResponse = hmcHearingApiService.sendCreateHearingRequest(hearingPayload);

            log.debug("Received Create Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
                caseId,
                wrapper.getHearingState().getState(),
                hmcUpdateResponse.toString());
        } else {
            hmcUpdateResponse = HmcUpdateResponse.builder()
                .hearingRequestId(hearing.getHearingId())
                .versionNumber(getHearingVersionNumber(hearing))
                .status(hearing.getHmcStatus())
                .build();

            log.debug("Existing hearing found, skipping Create Hearing Request for Case ID {}, Hearing State {}, Hearing version {} and "
                    + "Hearing Id {}",
                caseId,
                hearing.getHmcStatus(),
                hearing.getRequestVersion(),
                hearing.getHearingId());
        }

        hearingResponseUpdate(wrapper, hmcUpdateResponse);
    }

    private Long getHearingVersionNumber(CaseHearing hearing) {
        try {
            HearingGetResponse response = hmcHearingApiService.getHearingRequest(hearing.getHearingId().toString());
            return response.getRequestDetails().getVersionNumber();
        } catch (GetHearingException e) {
            log.debug("Hearing with id {} doesn't exist", hearing.getHearingId());
        }

        return HEARING_VERSION_NUMBER;
    }


}
