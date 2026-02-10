package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.REGULAR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class UploadDocumentSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Value("${feature.tribunal-internal-documents.enabled}")
    private final boolean isTribunalInternalDocumentsEnabled;

    UploadDocumentSubmittedCallbackHandler(UpdateCcdCaseService updateCcdCaseService,
                                           IdamService idamService,
                                           @Value("${feature.tribunal-internal-documents.enabled}")
                                           boolean isTribunalInternalDocumentsEnabled) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.isTribunalInternalDocumentsEnabled = isTribunalInternalDocumentsEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        if (isTribunalInternalDocumentsEnabled
            && REGULAR.equals(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentTo())
            && YES.equals(sscsCaseData.getInternalCaseDocumentData().getShouldBeIssued())) {
            String issueToAllParties = "Issue to all parties";
            log.info("Updating case using triggerCaseEventV2 for event: {}, event description: {}, caseId: {}",
                EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), issueToAllParties, callback.getCaseDetails().getId());
            updateCcdCaseService.triggerCaseEventV2(callback.getCaseDetails().getId(),
                EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), issueToAllParties,
                issueToAllParties, idamService.getIdamTokens());
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
