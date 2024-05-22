package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import java.util.Optional;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;

@RunWith(JUnitParamsRunner.class)
public class SubmitAppealServiceTestV2 extends AbstractSubmitAppealServiceTest {

    @Override
    public void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService, Long caseDetailsId, SaveCaseOperation saveCaseOperation) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(caseDetailsId)
                        .saveCaseOperation(saveCaseOperation)
                        .build());
    }

    @Override
    public void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyCitizenCcdService(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).saveCaseV2(any(IdamTokens.class), any(Consumer.class));
    }

    @Override
    public Optional<SaveCaseResult> callSubmitDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealServiceV2.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    @Override
    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCaseCitizenV2(any(String.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(Consumer.class)))
                .willReturn(caseDetails);
    }

    @Override
    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    @Override
    public Optional<SaveCaseResult> callUpdateDraftAppeal(SubmitAppealService submitAppealService, SubmitAppealServiceV2 submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealServiceV2.updateDraftAppeal(auth2Token, appealData);
    }

    @Override
    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(Consumer.class));
    }
}
