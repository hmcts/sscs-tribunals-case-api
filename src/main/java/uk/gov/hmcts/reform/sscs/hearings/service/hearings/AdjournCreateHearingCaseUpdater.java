package uk.gov.hmcts.reform.sscs.hearings.service.hearings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.hearings.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.hearings.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.hearings.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.hearings.service.exceptions.UpdateCcdCaseDetailsException;
import uk.gov.hmcts.reform.sscs.hearings.service.holder.ReferenceDataServiceHolder;


@Component
@Slf4j
public class AdjournCreateHearingCaseUpdater extends CreateHearingCaseUpdater {
    public AdjournCreateHearingCaseUpdater(CcdClient ccdClient,
                                           SscsCcdConvertService sscsCcdConvertService,
                                           HmcHearingApiService hmcHearingApiService, HmcHearingsApiService hmcHearingsApiService,
                                           CcdCaseService ccdCaseService, ReferenceDataServiceHolder refData,
                                           IdamService idamService) {
        super(ccdClient, sscsCcdConvertService, hmcHearingApiService, hmcHearingsApiService, ccdCaseService, refData, idamService);
    }

    @Override
    protected UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseData data, HearingWrapper hearingWrapper) throws UpdateCcdCaseDetailsException {
        hearingWrapper.setHearingState(HearingState.CREATE_HEARING);
        data.getAdjournment().setAdjournmentInProgress(YesNo.YES);
        return super.applyUpdate(data, hearingWrapper);
    }
}
