package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import java.util.*;
import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;


@RunWith(JUnitParamsRunner.class)
public class SubmitAppealServiceTestV1 extends AbstractSubmitAppealServiceTest {

    @Override
    public void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService, Long caseDetailsId, SaveCaseOperation saveCaseOperation) {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(caseDetailsId)
                        .saveCaseOperation(saveCaseOperation)
                        .build());
    }

    @Override
    public void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyCitizenCcdService(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Override
    public Optional<SaveCaseResult> callSubmitDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealService.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    @Override
    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willReturn(caseDetails);
    }

    @Override
    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willThrow(feignException);
    }

    @Override
    public Optional<SaveCaseResult> callUpdateDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealService.updateDraftAppeal(auth2Token, appealData);
    }

    @Override
    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any());
    }
}