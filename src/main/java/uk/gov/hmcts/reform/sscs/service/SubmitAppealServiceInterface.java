package uk.gov.hmcts.reform.sscs.service;

import feign.FeignException;
import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;

public interface SubmitAppealServiceInterface {
    String DM_STORE_USER_ID = "sscs";
    String CITIZEN_ROLE = "citizen";
    String DRAFT = "draft";
    String USER_HAS_A_INVALID_ROLE_MESSAGE = "User has a invalid role";

    Optional<SessionDraft> getDraftAppeal(String oauth2Token);

    List<SessionDraft> getDraftAppeals(String oauth2Token);

    Long submitAppeal(SyaCaseWrapper appeal, String userToken);

    Optional<SaveCaseResult> submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Boolean forceCreate);

    Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper);

    Optional<SaveCaseResult> archiveDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper, Long ccdCaseId) throws FeignException;
}
