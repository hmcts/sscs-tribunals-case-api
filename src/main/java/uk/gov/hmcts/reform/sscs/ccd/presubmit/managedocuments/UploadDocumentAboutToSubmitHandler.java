package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.REGULAR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments.UploadDocumentMidEventHandler.getDocumentIdFromUrl;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
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
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.UPLOAD_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (isTribunalInternalDocumentsEnabled) {
            if ("move".equalsIgnoreCase(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveOrMoveDocument())) {
                boolean moveToInternal = INTERNAL.equals(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentTo());
                DynamicMixedChoiceList dynamicList = sscsCaseData.getInternalCaseDocumentData().getDynamicList(moveToInternal);
                List<DynamicListItem> selectedOptions = dynamicList.getValue();
                if (emptyIfNull(selectedOptions).isEmpty()) {
                    PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
                    preSubmitCallbackResponse.addError("Please select at least one document to move");
                    return preSubmitCallbackResponse;
                }
                PreSubmitCallbackResponse<SscsCaseData> errorResponse = handleMove(sscsCaseData, moveToInternal, selectedOptions);
                if (!errorResponse.getErrors().isEmpty()) {
                    return errorResponse;
                }
            }
            setCaseDataAfterMoveUploadRemove(sscsCaseData);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setCaseDataAfterMoveUploadRemove(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument() != null) {
            sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument()
                .forEach(doc -> {
                    doc.getValue().setBundleAddition(null);
                    doc.getValue().setDocumentTabChoice(INTERNAL);
                });
        }
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveDocumentType(null);
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument(null);
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToDocumentsTabDL(null);
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToInternalDocumentsTabDL(null);
    }

    private void addToDocumentTabAndIssue(SscsCaseData sscsCaseData, SscsDocument docToMove, PreSubmitCallbackResponse<SscsCaseData> errorResponse) {
        if (!stream(DocumentType.values())
            .map(DocumentType::getValue).toList()
            .contains(docToMove.getValue().getDocumentType())) {
            errorResponse.addError("Document needs a valid Document Type to be moved: " + (isNotBlank(docToMove.getValue().getDocumentFileName())
                ? docToMove.getValue().getDocumentFileName()
                : docToMove.getValue().getDocumentLink().getDocumentFilename()));
        } else {
            addDocumentToDocumentTabAndBundle(footerService, sscsCaseData,
                docToMove.getValue().getDocumentLink(), DocumentType.fromValue(docToMove.getValue().getDocumentType()));
        }
    }

    private void moveDocumentToTribunalInternalDocuments(SscsCaseData sscsCaseData, SscsDocument docToMove) {
        removeDocumentFromCaseDataDocuments(sscsCaseData, docToMove);
        docToMove.getValue().setDocumentTabChoice(INTERNAL);
        addDocumentToCaseDataInternalDocuments(sscsCaseData, docToMove);
    }

    private void moveDocumentToDocuments(SscsCaseData sscsCaseData, SscsDocument docToMove, PreSubmitCallbackResponse<SscsCaseData> errorResponse) {
        removeDocumentFromCaseDataInternalDocuments(sscsCaseData, docToMove);
        docToMove.getValue().setDocumentTabChoice(REGULAR);
        if (isNoOrNull(sscsCaseData.getInternalCaseDocumentData().getShouldBeIssued())) {
            addDocumentToCaseDataDocuments(sscsCaseData, docToMove);
        } else {
            addToDocumentTabAndIssue(sscsCaseData, docToMove, errorResponse);
        }
    }

    private void handleMoveToDocuments(SscsCaseData sscsCaseData, SscsDocument docToMove, PreSubmitCallbackResponse<SscsCaseData> errorResponse, boolean moveToInternal) {
        if (moveToInternal) {
            moveDocumentToTribunalInternalDocuments(sscsCaseData, docToMove);
        } else {
            moveDocumentToDocuments(sscsCaseData, docToMove, errorResponse);
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> handleMove(SscsCaseData sscsCaseData, boolean moveToInternal, List<DynamicListItem> selectedOptions) {
        List<SscsDocument> docList = Optional.ofNullable(moveToInternal ? sscsCaseData.getSscsDocument()
            : sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument()).orElse(Collections.emptyList());
        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        for (DynamicListItem doc : selectedOptions) {
            docList.stream()
                .filter(d -> getDocumentIdFromUrl(d).equalsIgnoreCase(doc.getCode()))
                .findFirst()
                .ifPresentOrElse(
                    docToMove -> handleMoveToDocuments(sscsCaseData, docToMove, errorResponse, moveToInternal),
                    () -> errorResponse.addError("Document " + doc.getLabel() + " could not be found on the case.")
                );
        }
        if (!moveToInternal && YES.equals(sscsCaseData.getInternalCaseDocumentData().getShouldBeIssued())) {
            sscsCaseData.setDwpState(DwpState.FE_RECEIVED);
        }
        return errorResponse;
    }
}
