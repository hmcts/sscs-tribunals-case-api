package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
            .anyMatch(doc -> isNo(doc.getValue().getEvidenceIssued()));
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
        noIssuedEvidenceDocs.forEach(doc -> doc.getValue().setEvidenceIssued(YES));
    }

    @NotNull
    private List<SscsDocument> getNoIssuedEvidenceDocs(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            return Collections.emptyList();
        }
        return sscsDocument.stream()
            .filter(doc -> isNo(doc.getValue().getEvidenceIssued()))
            .collect(Collectors.toList());

    }
}
