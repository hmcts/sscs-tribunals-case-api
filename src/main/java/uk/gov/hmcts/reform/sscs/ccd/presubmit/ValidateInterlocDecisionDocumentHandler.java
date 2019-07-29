package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDecisionDocument;

@Slf4j
public class ValidateInterlocDecisionDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean canHandle = callbackType == CallbackType.ABOUT_TO_SUBMIT && (
                callback.getEvent() == EventType.TCW_DECISION_APPEAL_TO_PROCEED
                        || callback.getEvent() == EventType.JUDGE_DECISION_APPEAL_TO_PROCEED
        );
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        log.info("Can handle callback for case ["+ caseDetails.getCaseData().getCcdCaseId() + "]" +
                " for type [" + callbackType + "]" +
                " event [" + callback.getEvent() + "]" +
                " can handle [" + canHandle + "]");
        return canHandle;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        log.info("Checking for case [" + caseData.getCcdCaseId() + "] we have [" + caseData.getSscsInterlocDecisionDocument() + "]");
        if (caseData.getSscsInterlocDecisionDocument() == null || caseData.getSscsInterlocDecisionDocument().getDocumentDateAdded() == null) {
            sscsCaseDataPreSubmitCallbackResponse.addError("Interloc decision document must be set");
        } else if (!isPdf(caseData.getSscsInterlocDecisionDocument())) {
            sscsCaseDataPreSubmitCallbackResponse.addError("Interloc decision document must be a PDF");
        }

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private boolean isPdf(SscsInterlocDecisionDocument sscsInterlocDecisionDocument) {
        return sscsInterlocDecisionDocument.getDocumentLink().getDocumentFilename().toLowerCase().endsWith(".pdf");
    }
}
