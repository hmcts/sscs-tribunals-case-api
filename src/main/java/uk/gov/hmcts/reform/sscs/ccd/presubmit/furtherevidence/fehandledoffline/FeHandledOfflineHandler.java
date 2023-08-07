package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class FeHandledOfflineHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callback != null && EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE == callback.getEvent()
            && CallbackType.ABOUT_TO_SUBMIT == callbackType
            && (thereIsAnyDocumentToIssue(callback.getCaseDetails().getCaseData().getSscsDocument())
            || hmctsDwpStateFlagIsToClear(callback));
    }

    private boolean hmctsDwpStateFlagIsToClear(Callback<SscsCaseData> callback) {
        return "failedSendingFurtherEvidence".equals(callback.getCaseDetails().getCaseData().getHmctsDwpState());
    }

    private boolean thereIsAnyDocumentToIssue(List<SscsDocument> sscsDocuments) {
        return null != sscsDocuments && sscsDocuments.stream()
            .filter(doc -> doc.getValue() != null)
            .anyMatch(doc -> "No".equals(doc.getValue().getEvidenceIssued()));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        callback.getCaseDetails().getCaseData().setHmctsDwpState(null);
        setEvidenceIssuedFlag(callback);
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    private void setEvidenceIssuedFlag(Callback<SscsCaseData> callback) {
        List<SscsDocument> sscsDocument = callback.getCaseDetails().getCaseData().getSscsDocument();
        List<SscsDocument> noIssuedEvidenceDocs = getNoIssuedEvidenceDocs(sscsDocument);
        noIssuedEvidenceDocs.forEach(doc -> doc.getValue().setEvidenceIssued("Yes"));
    }

    @NotNull
    private List<SscsDocument> getNoIssuedEvidenceDocs(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            return Collections.emptyList();
        }
        return sscsDocument.stream()
            .filter(doc -> "No".equals(doc.getValue().getEvidenceIssued()))
            .toList();
    }
}
