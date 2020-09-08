package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class UploadWelshDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {

        nextEventMap.put(DocumentType.SSCS1.getValue(), EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(), EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(), EventType.DIRECTION_ISSUED_WELSH.getCcdType());
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.UPLOAD_WELSH_DOCUMENT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        updateCase(callback, caseData);
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        String previewDocumentType = null;
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            if (sscsDocument.getValue().getDocumentTranslationStatus() != null
                    && sscsDocument.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)) {
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                    sscsDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                }
            }
        }
        for (SscsWelshDocument sscsWelshPreviewDocument : caseData.getSscsWelshPreviewDocuments()) {
            sscsWelshPreviewDocument.getValue().setOriginalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode());
            previewDocumentType = sscsWelshPreviewDocument.getValue().getDocumentType();
            log.info("previewDocumentType  {}", previewDocumentType);
            if (caseData.getSscsWelshDocuments() != null) {
                caseData.getSscsWelshDocuments().add(sscsWelshPreviewDocument);
            } else {
                List<SscsWelshDocument> sscsWelshDocumentsList = new ArrayList<>();
                sscsWelshDocumentsList.add(sscsWelshPreviewDocument);
                caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
            }
        }

        //clear the Preview collection
        caseData.setSscsWelshPreviewDocuments(new ArrayList<>());
        caseData.updateTranslationWorkOutstandingFlag();
        String nextEvent = getNextEvent(previewDocumentType);
        log.info("Setting next event to {}", nextEvent);
        caseData.setSscsWelshPreviewNextEvent(nextEvent);
        return;
    }

    private String getNextEvent(String documentType) {
        return nextEventMap.get(documentType);
    }
}
