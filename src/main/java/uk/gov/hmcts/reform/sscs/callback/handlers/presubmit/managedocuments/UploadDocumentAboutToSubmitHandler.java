package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments.UploadDocumentMidEventHandler.getDocumentIdFromUrl;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.addDocumentToCaseDataDocuments;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.addDocumentToCaseDataInternalDocuments;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.addDocumentToDocumentTabAndBundle;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.removeDocumentFromCaseDataDocuments;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.removeDocumentFromCaseDataInternalDocuments;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadDocumentAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final FooterService footerService;

    @Value("${feature.tribunal-internal-documents.enabled}")
    private final boolean isTribunalInternalDocumentsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.UPLOAD_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(
        CallbackType callbackType,
        Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        if (!isTribunalInternalDocumentsEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        InternalCaseDocumentData internalCaseDocumentData = Optional.ofNullable(sscsCaseData.getInternalCaseDocumentData())
            .orElse(InternalCaseDocumentData.builder().build());
        if ("move".equalsIgnoreCase(internalCaseDocumentData.getUploadRemoveOrMoveDocument())) {
            boolean moveToInternal = INTERNAL.equals(internalCaseDocumentData.getMoveDocumentTo());
            DynamicMixedChoiceList dynamicList = moveToInternal
                ? internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL()
                : internalCaseDocumentData.getMoveDocumentToDocumentsTabDL();
            List<DynamicListItem> selectedOptions = dynamicList.getValue();
            if (emptyIfNull(selectedOptions).isEmpty()) {
                PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
                preSubmitCallbackResponse.addError("Please select at least one document to move");
                return preSubmitCallbackResponse;
            }
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = processMoveRequest(sscsCaseData, internalCaseDocumentData, moveToInternal, selectedOptions);
            if (!errorResponse.getErrors().isEmpty()) {
                return errorResponse;
            }
        }
        resetInternalCaseDocumentData(internalCaseDocumentData);
        sscsCaseData.setInternalCaseDocumentData(internalCaseDocumentData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }


    private PreSubmitCallbackResponse<SscsCaseData> processMoveRequest(SscsCaseData sscsCaseData, InternalCaseDocumentData internalCaseDocumentData, boolean moveToInternal, List<DynamicListItem> selectedOptions) {
        List<SscsDocument> docList = Optional.ofNullable(moveToInternal ? sscsCaseData.getSscsDocument() : internalCaseDocumentData.getSscsInternalDocument())
            .orElse(Collections.emptyList());
        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        List<String> documentTypeList = stream(DocumentType.values()).map(DocumentType::getValue).toList();
        processSelectedDocuments(sscsCaseData, internalCaseDocumentData, moveToInternal, selectedOptions, docList, errorResponse, documentTypeList);
        updateDwpState(sscsCaseData, internalCaseDocumentData, moveToInternal);
        return errorResponse;
    }

    private void processSelectedDocuments(SscsCaseData sscsCaseData, InternalCaseDocumentData internalCaseDocumentData, boolean moveToInternal, List<DynamicListItem> selectedOptions, List<SscsDocument> docList, PreSubmitCallbackResponse<SscsCaseData> errorResponse, List<String> documentTypeList) {
        for (DynamicListItem doc : selectedOptions) {
            docList.stream()
                .filter(d -> getDocumentIdFromUrl(d).equalsIgnoreCase(doc.getCode()))
                .findFirst()
                .ifPresentOrElse(
                    docToMove -> moveDocument(sscsCaseData, internalCaseDocumentData, documentTypeList, docToMove, errorResponse, moveToInternal),
                    () -> errorResponse.addError("Document " + doc.getLabel() + " could not be found on the case.")
                );
        }
    }

    private void moveDocument(SscsCaseData sscsCaseData, InternalCaseDocumentData internalCaseDocumentData, List<String> documentTypeList, SscsDocument docToMove, PreSubmitCallbackResponse<SscsCaseData> errorResponse, boolean moveToInternal) {
        if (moveToInternal) {
            removeDocumentFromCaseDataDocuments(sscsCaseData, docToMove);
            addDocumentToCaseDataInternalDocuments(sscsCaseData, docToMove);
        } else {
            removeDocumentFromCaseDataInternalDocuments(sscsCaseData, docToMove);
            if (isNoOrNull(internalCaseDocumentData.getShouldBeIssued())) {
                addDocumentToCaseDataDocuments(sscsCaseData, docToMove);
            } else {
                addToDocumentTabAndIssue(sscsCaseData, documentTypeList, docToMove, errorResponse);
            }
        }
    }

    private void addToDocumentTabAndIssue(SscsCaseData sscsCaseData, List<String> documentTypeList, SscsDocument docToMove, PreSubmitCallbackResponse<SscsCaseData> errorResponse) {
        if (!documentTypeList
            .contains(docToMove.getValue().getDocumentType())) {
            errorResponse.addError("Document needs a valid Document Type to be moved: " + (isNotBlank(docToMove.getValue().getDocumentFileName())
                ? docToMove.getValue().getDocumentFileName()
                : docToMove.getValue().getDocumentLink().getDocumentFilename()));
        } else {
            addDocumentToDocumentTabAndBundle(footerService, sscsCaseData, docToMove.getValue().getDocumentLink(),
                DocumentType.fromValue(docToMove.getValue().getDocumentType()), null, true);
        }
    }

    private void updateDwpState(SscsCaseData sscsCaseData, InternalCaseDocumentData internalCaseDocumentData, boolean moveToInternal) {
        if (!moveToInternal && YES.equals(internalCaseDocumentData.getShouldBeIssued())) {
            sscsCaseData.setDwpState(DwpState.FE_RECEIVED);
        }
    }

    private void resetInternalCaseDocumentData(InternalCaseDocumentData internalCaseDocumentData) {
        emptyIfNull(internalCaseDocumentData.getSscsInternalDocument())
            .forEach(doc -> {
                doc.getValue().setBundleAddition(null);
                doc.getValue().setEvidenceIssued(null);
            });
        internalCaseDocumentData.setUploadRemoveDocumentType(null);
        internalCaseDocumentData.setUploadRemoveOrMoveDocument(null);
        internalCaseDocumentData.setMoveDocumentToDocumentsTabDL(null);
        internalCaseDocumentData.setMoveDocumentToInternalDocumentsTabDL(null);
        if (emptyIfNull(internalCaseDocumentData.getSscsInternalDocument()).isEmpty()) {
            internalCaseDocumentData.setSscsInternalDocument(null);
        }
    }
}
