package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;

@Service
@Slf4j
public class UploadWelshDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private WelshFooterService welshFooterService;
    private BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder;

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {
        nextEventMap.put(DocumentType.SSCS1.getValue(), EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.URGENT_HEARING_REQUEST.getValue(), EventType.UPDATE_CASE_ONLY.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(), EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(), EventType.DIRECTION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.REINSTATEMENT_REQUEST.getValue(), EventType.UPDATE_CASE_ONLY.getCcdType());
    }

    @Autowired
    public UploadWelshDocumentsAboutToSubmitHandler(WelshFooterService welshFooterService, BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder) {
        this.welshFooterService = welshFooterService;
        this.bundleAdditionFilenameBuilder = bundleAdditionFilenameBuilder;
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
        log.info("About to submit Upload Welsh Document for caseID:  {}", caseData.getCcdCaseId());
        PreSubmitCallbackResponse<SscsCaseData>  preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        if (caseData.getSscsWelshPreviewDocuments() == null ||  caseData.getSscsWelshPreviewDocuments().isEmpty()
            || (caseData.getSscsWelshPreviewDocuments().get(0).getValue().getDocumentLink() == null)) {
            preSubmitCallbackResponse.addError("Please select a document to upload");
            return preSubmitCallbackResponse;
        }
        updateCase(callback, caseData);
        return preSubmitCallbackResponse;
    }

    private void updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        String previewDocumentType = null;
        log.info("Set the Translation Status to complete for originalDocs for caseID:  {}", caseData.getCcdCaseId());
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            if (sscsDocument.getValue().getDocumentTranslationStatus() != null
                && sscsDocument.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)) {
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                    sscsDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                }
            }
        }
        log.info("Set up welsh document for caseId:  {}", caseData.getCcdCaseId());
        for (SscsWelshDocument sscsWelshPreviewDocument : caseData.getSscsWelshPreviewDocuments()) {
            sscsWelshPreviewDocument.getValue().setOriginalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode());
            previewDocumentType = sscsWelshPreviewDocument.getValue().getDocumentType();
            log.info("previewDocumentType  {}", previewDocumentType);
            if (sscsWelshPreviewDocument.getValue().getDocumentDateAdded() == null) {
                sscsWelshPreviewDocument.getValue().setDocumentDateAdded(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
            }
            if (DocumentType.APPELLANT_EVIDENCE.getValue().equals(previewDocumentType)) {
                log.info("Set up Appellant Evidence welsh document for caseId:  {}", caseData.getCcdCaseId());
                Optional<SscsDocument> sscsDocumentByTypeAndName = getSscsDocumentByTypeAndName(DocumentType.APPELLANT_EVIDENCE, sscsWelshPreviewDocument.getValue().getOriginalDocumentFileName(), caseData);
                sscsDocumentByTypeAndName.ifPresent(sscsDocument -> {
                    if (StringUtils.isNotEmpty(sscsDocument.getValue().getBundleAddition())) {
                        log.info("Adding bundle addition for  appelant evidence for caseId:  {}", caseData.getCcdCaseId());
                        setBundleAdditionDetails(caseData, sscsWelshPreviewDocument);
                    }
                });
            }
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
        if (!callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)) {
            String nextEvent = getNextEvent(caseData, previewDocumentType);
            log.info("Setting next event to {}", nextEvent);
            caseData.setSscsWelshPreviewNextEvent(nextEvent);
        } else if (!caseData.isTranslationWorkOutstanding()) {
            caseData.setInterlocReviewState(caseData.getWelshInterlocNextReviewState());
            caseData.setWelshInterlocNextReviewState(null);
        }
        return;
    }

    private void setBundleAdditionDetails(SscsCaseData caseData, SscsWelshDocument sscsWelshPreviewDocument) {
        String documentFooterText = "Appellant evidence";
        String bundleAddition = welshFooterService.getNextBundleAddition(caseData.getSscsWelshDocuments());
        DocumentLink newUrl = welshFooterService.addFooter(sscsWelshPreviewDocument.getValue().getDocumentLink(), documentFooterText, bundleAddition);

        String fileName = bundleAdditionFilenameBuilder.build(DocumentType.APPELLANT_EVIDENCE, bundleAddition, sscsWelshPreviewDocument.getValue().getDocumentDateAdded(), DateTimeFormatter.ISO_LOCAL_DATE);
        sscsWelshPreviewDocument.getValue().setDocumentFileName(fileName);
        sscsWelshPreviewDocument.getValue().setDocumentLink(newUrl);
        sscsWelshPreviewDocument.getValue().setEvidenceIssued("No");
        sscsWelshPreviewDocument.getValue().setBundleAddition(bundleAddition);

    }


    private Optional<SscsDocument> getSscsDocumentByTypeAndName(DocumentType documentType, String fileName, SscsCaseData caseData) {
        return Optional.ofNullable(caseData.getSscsDocument()).map(Collection::stream).orElseGet(Stream::empty)
            .filter(doc -> (doc.getValue().getDocumentType().equals(documentType.getValue())
                    || doc.getValue().getDocumentType().equals(DocumentType.OTHER_DOCUMENT.getValue()))
                    && doc.getValue().getDocumentLink().getDocumentFilename().equals(fileName))
            .sorted()
            .findFirst();
    }

    private String getNextEvent(SscsCaseData caseData, String documentType) {
        return nextEventMap.get(documentType);
    }
}
