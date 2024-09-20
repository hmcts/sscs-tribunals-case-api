package uk.gov.hmcts.reform.sscs.service.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.service.AbstractSubmitAppealServiceTest;

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
    public Optional<SaveCaseResult> callSubmitDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealServiceV2.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    @Override
    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCaseCitizenV2(any(String.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willReturn(caseDetails);
    }

    @Override
    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willThrow(feignException);
    }

    @Override
    public Optional<SaveCaseResult> callUpdateDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealServiceV2.updateDraftAppeal(auth2Token, appealData);
    }

    @Override
    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class));
    }

    @Override
    public Optional<SaveCaseResult> callArchiveDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, Long caseId) {
        return submitAppealServiceV2.archiveDraftAppeal(auth2Token, appealData, caseId);
    }

    @Override
    public void verifyArchiveDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
    }

    @Override
    public void givenArchiveDraftAppealWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willReturn(caseDetails);
    }

    @Override
    public void givenArchiveDraftAppealWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyArchiveDraft(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
    }
}
