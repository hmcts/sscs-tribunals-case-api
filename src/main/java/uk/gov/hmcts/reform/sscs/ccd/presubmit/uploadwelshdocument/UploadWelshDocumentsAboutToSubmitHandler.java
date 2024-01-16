package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;

@Service
@Slf4j
public class UploadWelshDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final WelshFooterService welshFooterService;

    private static final Map<String, String> nextEventMap = new HashMap<>();
    private static final String WELSH_ADDITION_PREFIX = "WEL-";

    static {
        nextEventMap.put(DocumentType.SSCS1.getValue(), EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(), EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(), EventType.DIRECTION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.FINAL_DECISION_NOTICE.getValue(), EventType.ISSUE_FINAL_DECISION_WELSH.getCcdType());
    }

    @Autowired
    public UploadWelshDocumentsAboutToSubmitHandler(WelshFooterService welshFooterService) {
        this.welshFooterService = welshFooterService;
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

        log.info("Set the Translation Status to complete for originalDocs for caseId: {}", caseData.getCcdCaseId());
        updateTranslationStatusOfSscsDocument(caseData);
        updateTranslationStatusOfDwpDocument(caseData);

        log.info("Set up welsh document for caseId:  {}", caseData.getCcdCaseId());
        for (SscsWelshDocument sscsWelshPreviewDocument : caseData.getSscsWelshPreviewDocuments()) {
            sscsWelshPreviewDocument.getValue().setOriginalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode());
            if (sscsWelshPreviewDocument.getValue().getDocumentDateAdded() == null) {
                sscsWelshPreviewDocument.getValue().setDocumentDateAdded(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
            }
            log.info("Set up welsh document for caseId:  {}", caseData.getCcdCaseId());
            Optional<SscsDocument> sscsDocumentByTypeAndName = getSscsDocumentByFilename(sscsWelshPreviewDocument.getValue().getOriginalDocumentFileName(), caseData);
            sscsDocumentByTypeAndName.ifPresent(sscsDocument -> {
                if (StringUtils.isNotEmpty(sscsDocument.getValue().getBundleAddition())) {
                    log.info("Adding bundle addition for appellant evidence for caseId:  {}", caseData.getCcdCaseId());
                    setBundleAdditionDetails(sscsWelshPreviewDocument, sscsDocument);
                }
            });
            if (caseData.getSscsWelshDocuments() != null) {
                caseData.getSscsWelshDocuments().add(sscsWelshPreviewDocument);
            } else {
                List<SscsWelshDocument> sscsWelshDocumentsList = new ArrayList<>();
                sscsWelshDocumentsList.add(sscsWelshPreviewDocument);
                caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
            }
            if (!callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)) {
                String nextEvent = getNextEvent(sscsWelshPreviewDocument.getValue().getDocumentType());
                log.info("Setting next event to {}", nextEvent);
                caseData.setSscsWelshPreviewNextEvent(nextEvent);
            } else if (!caseData.isTranslationWorkOutstanding()) {
                InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                    .filter(x -> x.getCcdDefinition().equals(caseData.getWelshInterlocNextReviewState()))
                    .findFirst()
                    .orElse(null);
                caseData.setInterlocReviewState(interlocState);
                caseData.setWelshInterlocNextReviewState(null);
            }
        }


        //clear the Preview collection
        caseData.setSscsWelshPreviewDocuments(new ArrayList<>());
        caseData.updateTranslationWorkOutstandingFlag();

    }

    private void updateTranslationStatusOfSscsDocument(SscsCaseData caseData) {
        if (caseData.getSscsDocument() != null) {
            for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
                if (SscsDocumentTranslationStatus.TRANSLATION_REQUESTED.equals(sscsDocument.getValue().getDocumentTranslationStatus())) {
                    if (sscsDocument.getValue().getDocumentLink() != null && sscsDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                        sscsDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                    }
                }
            }
        }
    }

    private void updateTranslationStatusOfDwpDocument(SscsCaseData caseData) {
        if (caseData.getDwpDocuments() != null) {
            for (DwpDocument dwpDocument : caseData.getDwpDocuments()) {
                if (SscsDocumentTranslationStatus.TRANSLATION_REQUESTED.equals(dwpDocument.getValue().getDocumentTranslationStatus())) {
                    if (dwpDocument.getValue().getDocumentLink() != null && dwpDocument.getValue().getDocumentLink().getDocumentFilename().equals(caseData.getOriginalDocuments().getValue().getCode())) {
                        dwpDocument.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
                    }
                }
            }
        }
    }

    private void setBundleAdditionDetails(SscsWelshDocument sscsWelshPreviewDocument, SscsDocument sscsDocument) {
        String bundleAddition = WELSH_ADDITION_PREFIX + sscsDocument.getValue().getBundleAddition();
        DocumentLink newUrl = welshFooterService.addFooter(sscsWelshPreviewDocument.getValue().getDocumentLink(), DocumentType.fromValue(sscsDocument.getValue().getDocumentType()).getLabel(), bundleAddition);

        String fileName = buildWelshFileNameFromOriginal(sscsDocument.getValue().getDocumentFileName());
        sscsWelshPreviewDocument.getValue().setDocumentFileName(fileName);
        sscsWelshPreviewDocument.getValue().setDocumentLink(newUrl);
        sscsWelshPreviewDocument.getValue().setEvidenceIssued("No");
        sscsWelshPreviewDocument.getValue().setBundleAddition(bundleAddition);
    }

    private String buildWelshFileNameFromOriginal(String documentFileName) {
        String[] originalFileNameSplit = documentFileName.split("Addition ");
        if (originalFileNameSplit.length == 2) {
            return "Addition " + WELSH_ADDITION_PREFIX + originalFileNameSplit[1];
        } else {
            return documentFileName;
        }
    }


    private Optional<SscsDocument> getSscsDocumentByFilename(String filename, SscsCaseData caseData) {
        return Optional.ofNullable(caseData.getSscsDocument()).stream().flatMap(Collection::stream)
            .filter(doc -> doc.getValue().getDocumentLink().getDocumentFilename().equals(filename))
            .sorted()
            .findFirst();
    }

    private String getNextEvent(String documentType) {
        return nextEventMap.get(documentType);
    }
}
