package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class UploadDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT)
            && isValidDocumentType(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private boolean isValidDocumentType(List<SscsDocument> sscsDocuments) {
        if (sscsDocuments != null) {
            return sscsDocuments.stream()
                .anyMatch(doc -> {
                    String docType = doc.getValue() != null ? doc.getValue().getDocumentType() : null;
                    return DocumentType.MEDICAL_EVIDENCE.getId().equals(docType)
                        || DocumentType.OTHER_EVIDENCE.getId().equals(docType)
                        || DocumentType.APPELLANT_EVIDENCE.getId().equals(docType)
                        || DocumentType.REPRESENTATIVE_EVIDENCE.getId().equals(docType);
                });
        }
        return false;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpState(DwpState.FE_RECEIVED.getValue());
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
