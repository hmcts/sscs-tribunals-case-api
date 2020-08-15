package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class UploadWelshDocumentsSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {

        nextEventMap.put(DocumentType.SSCS1.getValue(),EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.APPELLANT_EVIDENCE.getValue(),EventType.UPLOAD_WELSH_DOCUMENT.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(),EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(),EventType.DIRECTION_ISSUED_WELSH.getCcdType());
    }

    @Autowired
    public UploadWelshDocumentsSubmittedCallbackHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent().equals(EventType.UPLOAD_WELSH_DOCUMENT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = updateCase(callback, caseData);

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }

    private SscsCaseDetails updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        int outStandingDocumentFlag = 0;
        String previewDocumentType = null;
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            if (sscsDocument.getValue().getDocumentTranslationStatus() != null
                    && sscsDocument.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)) {
                outStandingDocumentFlag++;
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                    sscsDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                }
            }
        }
        log.info("outStandingDocumentFlag  {}",outStandingDocumentFlag);
        for (SscsWelshDocument sscsWelshPreviewDocument : caseData.getSscsWelshPreviewDocuments()) {
            sscsWelshPreviewDocument.getValue().setOriginalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode());
            previewDocumentType = sscsWelshPreviewDocument.getValue().getDocumentType();
            log.info("previewDocumentType  {}",previewDocumentType);
            if (caseData.getSscsWelshDocuments() != null) {
                caseData.getSscsWelshDocuments().add(sscsWelshPreviewDocument);
            } else {
                List<SscsWelshDocument> sscsWelshDocumentsList =  new ArrayList<>();
                sscsWelshDocumentsList.add(sscsWelshPreviewDocument);
                caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
            }
        }

        //clear the Preview collection
        caseData.setSscsWelshPreviewDocuments(new ArrayList<>());
        if (outStandingDocumentFlag <= 1) {
            log.info("outStandingDocumentFlag if {}",outStandingDocumentFlag);
            caseData.setTranslationWorkOutstanding("No");
        } else {
            log.info("outStandingDocumentFlag else {}",outStandingDocumentFlag);
            caseData.setTranslationWorkOutstanding(caseData.getTranslationWorkOutstanding() != null
                    ? caseData.getTranslationWorkOutstanding() : "Yes");
        }

        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                getNextEvent(previewDocumentType), "Update document translation status",
                "Update document translation status", idamService.getIdamTokens());
    }

    private String getNextEvent(String documentType) {
        return nextEventMap.get(documentType) != null ? nextEventMap.get(documentType) :
                EventType.UPLOAD_WELSH_DOCUMENT.getCcdType();
    }
}
