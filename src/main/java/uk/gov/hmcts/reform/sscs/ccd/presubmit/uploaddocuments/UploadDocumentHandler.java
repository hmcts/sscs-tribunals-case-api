package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class UploadDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT)
            && callback.getCaseDetails().getCaseData().getSscsDocument() != null
            && callback.getCaseDetails().getCaseData().getSscsDocument().stream()
            .anyMatch(doc -> {
                String docType = doc.getValue().getDocumentType();
                return "Medical evidence".equals(docType)
                    || "Other evidence".equals(docType)
                    || "appellantEvidence".equals(docType)
                    || "representativeEvidence".equals(docType);
            });
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
