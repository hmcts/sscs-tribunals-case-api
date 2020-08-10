package uk.gov.hmcts.reform.sscs.ccd.presubmit.welsh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocuments;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
public class UploadWelshDocumentsSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {
        nextEventMap.put("sscs1","sendToDwp");
        nextEventMap.put("sscs1","sendToDwp");
        nextEventMap.put("sscs1","sendToDwp");
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
        String nextEvent;
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            if (sscsDocument.getValue().getDocumentTranslationStatus() !=null &&
                    sscsDocument.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)) {
                outStandingDocumentFlag++;
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                    sscsDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                }
            }
        }
        log.info("outStandingDocumentFlag  {}",outStandingDocumentFlag);
        for (SscsWelshDocuments sscsWelshPreviewDocuments : caseData.getSscsWelshPreviewDocuments()) {
            sscsWelshPreviewDocuments.getValue().setOriginalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode());
            nextEvent = sscsWelshPreviewDocuments.getValue().getDocumentType();
            log.info("nextEvent  {}",nextEvent);
            if(caseData.getSscsWelshDocuments() != null) {
                caseData.getSscsWelshDocuments().add(sscsWelshPreviewDocuments);
            } else {
                List<SscsWelshDocuments> sscsWelshDocumentsList =  new ArrayList<>();
                sscsWelshDocumentsList.add(sscsWelshPreviewDocuments);
                caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
            }
        }

        //clear the Preview collection
        caseData.setSscsWelshPreviewDocuments(new ArrayList<>());
        if(outStandingDocumentFlag <= 1) {
            log.info("outStandingDocumentFlag if {}",outStandingDocumentFlag);
            caseData.setTranslationWorkOutstanding("No");
        } else {
            log.info("outStandingDocumentFlag else {}",outStandingDocumentFlag);
            caseData.setTranslationWorkOutstanding(caseData.getTranslationWorkOutstanding() != null ?
                    caseData.getTranslationWorkOutstanding() : "Yes");
        }

        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.UPLOAD_WELSH_DOCUMENT.getCcdType(), "Update document translation status",
                "Update document translation status", idamService.getIdamTokens());
    }

    private String getNextEvent(String documentType){
        return nextEventMap.get(documentType) != null ? nextEventMap.get(documentType):
                EventType.UPLOAD_WELSH_DOCUMENT.getCcdType();
    }
}
