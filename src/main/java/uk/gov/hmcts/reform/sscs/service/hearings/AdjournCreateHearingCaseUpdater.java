package uk.gov.hmcts.reform.sscs.service.hearings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;


@Component
@Slf4j
public class AdjournCreateHearingCaseUpdater extends CreateHearingCaseUpdater {
    public AdjournCreateHearingCaseUpdater(CcdClient ccdClient,
                                           SscsCcdConvertService sscsCcdConvertService,
                                           HmcHearingApiService hmcHearingApiService, HmcHearingsApiService hmcHearingsApiService,
                                           ReferenceDataServiceHolder refData,
                                           IdamService idamService) {
        super(ccdClient, sscsCcdConvertService, hmcHearingApiService, hmcHearingsApiService, refData, idamService);
    }

    @Override
    protected UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseDetails caseDetails, HearingRequest hearingRequest) throws ListingException {
        log.info("Setting adjournment fields for Case ID {}, Case State {} and Hearing State {}",
                 caseDetails.getId(),
                 caseDetails.getState(),
                 hearingRequest.getHearingState().getState());

        caseDetails.getData().getAdjournment().setAdjournmentInProgress(YesNo.YES);
        return super.applyUpdate(caseDetails, hearingRequest);
    }
}
