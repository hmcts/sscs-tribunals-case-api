package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import java.time.LocalDate;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class UploadDocumentFurtherEvidenceHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (callbackType != null && callback != null) {
            boolean canBeHandle = callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE)
                && isValidDocumentType(callback.getCaseDetails().getCaseData().getDraftSscsFEDocument());
            if (!canBeHandle) {
                initDraftSscsFEDocument(callback.getCaseDetails().getCaseData());
            }
            return canBeHandle;
        }
        return false;
    }

    private boolean isValidDocumentType(List<SscsDocument> draftSscsFEDocuments) {
        if (draftSscsFEDocuments != null) {
            return draftSscsFEDocuments.stream()
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
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        moveDraftsToSscsDocs(caseData);
        initDraftSscsFEDocument(caseData);
        caseData.setDwpState(DwpState.FE_RECEIVED.getId());
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void initDraftSscsFEDocument(SscsCaseData caseData) {
        caseData.setDraftSscsFEDocument(null);
    }

    private void moveDraftsToSscsDocs(SscsCaseData caseData) {
        caseData.getDraftSscsFEDocument().forEach(draftDoc -> {
            if (StringUtils.isBlank(draftDoc.getValue().getDocumentDateAdded())) {
                draftDoc.getValue().setDocumentDateAdded(LocalDate.now().toString());
            }
            caseData.getSscsDocument().add(draftDoc);
        });
    }
}
