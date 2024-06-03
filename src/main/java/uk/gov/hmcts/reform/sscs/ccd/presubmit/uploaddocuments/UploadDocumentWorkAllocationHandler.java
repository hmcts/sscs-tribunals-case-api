package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@Service
@Slf4j
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
        addedDocumentsUtil.clearAddedDocumentsBeforeEventSubmit(caseData);

        List<String> addedDocumentTypes = addedDocumentsUtil.addedDocumentTypes(
            previousSscsDocuments(callback.getCaseDetailsBefore()),
            caseData.getSscsDocument()
        );

        List<String> addedScannedDocumentTypes = addedDocumentsUtil.addedDocumentTypes(
                previousScannedDocuments(callback.getCaseDetailsBefore()),
                caseData.getScannedDocuments()
        );

        List<String> addedAudioVideoTypes = addedDocumentsUtil.addedDocumentTypes(
            previousEvidence(callback.getCaseDetailsBefore()),
            caseData.getAudioVideoEvidence()
        );

        addedDocumentsUtil.updateScannedDocumentTypes(caseData,
                Stream.of(addedDocumentTypes, addedAudioVideoTypes, addedScannedDocumentTypes)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()));

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private List<? extends TypedDocument> previousSscsDocuments(Optional<CaseDetails<SscsCaseData>> caseData) {
        return caseData.isPresent() ? caseData.get().getCaseData().getSscsDocument() : null;
    }

    private List<? extends TypedDocument> previousScannedDocuments(Optional<CaseDetails<SscsCaseData>> caseData) {
        return caseData.isPresent() ? caseData.get().getCaseData().getScannedDocuments() : null;
    }

    private List<? extends TypedDocument> previousEvidence(Optional<CaseDetails<SscsCaseData>> caseData) {
        return caseData.isPresent() ? caseData.get().getCaseData().getAudioVideoEvidence() : null;
    }
}