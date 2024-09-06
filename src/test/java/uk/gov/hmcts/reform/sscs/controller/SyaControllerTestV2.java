package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceV2;

public class SyaControllerTestV2 extends AbstractSyaControllerTest {

    @Override
    boolean v2SubmitDraftAppealIsEnable() {
        return true;
    }

    @Override
    boolean v2UpdateDraftAppealIsEnable() {
        return true;
    }

    @Override
    boolean isArchiveDraftAppealV2Enabled() {
        return true;
    }

    @Override
    void mockSubmitAppealService(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2,
                                 Long caseId, SaveCaseOperation saveCaseOperation) {
        when(submitAppealServiceV2.submitDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(caseId)
                        .saveCaseOperation(saveCaseOperation)
                        .build()));
    }

    @Override
    public void mockSubmitAppealServiceUpdateDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, Long caseId, SaveCaseOperation saveCaseOperation) {
        when(submitAppealServiceV2.updateDraftAppeal(any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(caseId)
                        .saveCaseOperation(saveCaseOperation)
                        .build()));
    }

    @Override
    void mockSubmitAppealArchiveDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, Long caseId, SaveCaseOperation saveCaseOperation) {
        when(submitAppealServiceV2.archiveDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(caseId)
                        .saveCaseOperation(saveCaseOperation)
                        .build()));
    }
}
