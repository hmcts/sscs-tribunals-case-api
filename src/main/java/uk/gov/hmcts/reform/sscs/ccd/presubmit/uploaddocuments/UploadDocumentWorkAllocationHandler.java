package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Component
public class UploadDocumentWorkAllocationHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final AddedDocumentsUtil addedDocumentsUtil;

    private final boolean workAllocationFeature;

    public UploadDocumentWorkAllocationHandler(AddedDocumentsUtil addedDocumentsUtil, @Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.addedDocumentsUtil = addedDocumentsUtil;
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.UPLOAD_DOCUMENT
                && workAllocationFeature;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        List<String> addedDocumentTypes = addedDocumentTypes(
                previousSscsDocuments(callback.getCaseDetailsBefore()),
                caseData.getSscsDocument()
        );

        addedDocumentsUtil.clearAddedDocumentsBeforeEventSubmit(caseData);
        addedDocumentsUtil.computeDocumentsAddedThisEvent(caseData, addedDocumentTypes, callback.getEvent());

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private List<String> addedDocumentTypes(List<? extends AbstractDocument> previousDocuments, List<? extends AbstractDocument> documents) {
        Map<String, String> existingDocumentTypes = null;
        if (previousDocuments != null) {
            existingDocumentTypes = previousDocuments.stream().collect(
                    Collectors.toMap(d -> d.toString(), d -> d.getValue().getDocumentType()));
        }

        return addedDocumentTypes(existingDocumentTypes, documents);
    }

    public List<String> addedDocumentTypes(Map<String, String> existingDocumentTypes, List<? extends AbstractDocument> documents) {
        if (documents != null) {
            return documents.stream()
                    .filter(d -> isNewDocumentOrTypeChanged(existingDocumentTypes, d))
                    .map(d -> d.getValue().getDocumentType())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return null;
    }

    private boolean isNewDocumentOrTypeChanged(Map<String, String> existingDocumentTypes, AbstractDocument document) {
        if (existingDocumentTypes != null) {
            if (existingDocumentTypes.containsKey(document.toString())) {
                return !StringUtils.equals(document.getValue().getDocumentType(),
                        existingDocumentTypes.get(document.toString()));
            }
        }
        return true;
    }

    private List<? extends AbstractDocument> previousSscsDocuments(Optional<CaseDetails<SscsCaseData>> caseData) {
        return caseData.isPresent() ? caseData.get().getCaseData().getSscsDocument() : null;
    }
}
