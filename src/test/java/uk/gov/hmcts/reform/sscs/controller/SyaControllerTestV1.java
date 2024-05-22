package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceV2;

public class SyaControllerTestV1 extends AbstractSyaControllerTest {

    @Override
    boolean v2IsEnabled() {
        return false;
    }

    @Override
    boolean isArchiveDraftAppealV2Enabled() {
        return false;
    }

    @Override
    void mockSubmitAppealService(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2,
                                 Long caseId, SaveCaseOperation saveCaseOperation) {
        when(submitAppealService.submitDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(caseId)
                        .saveCaseOperation(saveCaseOperation)
                        .build()));
    }

    @Override
    void mockSubmitAppealArchiveDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2,
                                 Long caseId, SaveCaseOperation saveCaseOperation) {
        when(submitAppealService.archiveDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(caseId)
                        .saveCaseOperation(saveCaseOperation)
                        .build()));
    }
}