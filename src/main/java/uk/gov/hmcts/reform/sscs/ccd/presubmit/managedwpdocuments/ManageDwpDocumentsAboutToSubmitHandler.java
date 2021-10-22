package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
@Slf4j
public class ManageDwpDocumentsAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DwpDocumentService dwpDocumentService;

    @Autowired
    public ManageDwpDocumentsAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.MANAGE_DWP_DOCUMENTS;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        removeDwpDocumentsStoredInTheOldLocationTheyDoNotGetRemoveFromTheAboutToStartHandler(sscsCaseData);

        return validateDwpDocuments(sscsCaseData);
    }

    private void removeDwpDocumentsStoredInTheOldLocationTheyDoNotGetRemoveFromTheAboutToStartHandler(SscsCaseData sscsCaseData) {
        dwpDocumentService.removeOldDwpDocuments(sscsCaseData);
    }

    private void validateEditedEvidenceReason(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        sscsCaseData.getDwpDocuments().stream().forEach(dwpDocument -> {

            dwpDocumentService.validateEditedEvidenceReason(sscsCaseData, preSubmitCallbackResponse,
                    dwpDocument.getValue().getDwpEditedEvidenceReason());
        });

    }

    private PreSubmitCallbackResponse<SscsCaseData> validateDwpDocuments(SscsCaseData sscsCaseData) {
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        validateEditedEvidenceReason(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpResponseDocument(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);

        if (sscsCaseData.getDwpEditedEvidenceReason() != null) {
            validateEditedResponseAndBundle(sscsCaseData, preSubmitCallbackResponse);
        }
        return preSubmitCallbackResponse;
    }

    private void validateOneDwpResponseDocument(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpResponses = countDwpDocument(sscsCaseData, DwpDocumentType.DWP_RESPONSE);
        if (numberOfDwpResponses > 1) {
            preSubmitCallbackResponse.addError("Only one DWP response should be seen against each case, please correct");
        }
    }

    private void validateOneDwpEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpEvidenceBundleDocument = countDwpDocument(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);
        if (numberOfDwpEvidenceBundleDocument > 1) {
            preSubmitCallbackResponse.addError("Only one DWP evidence bundle should be seen against each case, please correct");
        }
    }

    private void validateEditedResponseAndBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        validateDwpEditedResponseIfThereIsAResponse(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpEditedResponse(sscsCaseData, preSubmitCallbackResponse);

        validateDwpEditedEvidenceBundleIfThereIsAnEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpEditedEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);
    }

    private void validateDwpEditedResponseIfThereIsAResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpResponseDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_RESPONSE);
        if (!isDocumentEmpty(dwpResponseDocument)) {
            validateDwpEditedResponse(sscsCaseData, preSubmitCallbackResponse);
        }
    }

    private void validateDwpEditedResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEditedResponseDocument = getEditedDwpDocument(sscsCaseData, DwpDocumentType.DWP_RESPONSE);

        if (isDocumentEmpty(dwpEditedResponseDocument)) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP response document");
        }
    }

    private void validateOneDwpEditedResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpEditedResponses = countEditedDwpDocument(sscsCaseData, DwpDocumentType.DWP_RESPONSE);
        if (numberOfDwpEditedResponses > 1) {
            preSubmitCallbackResponse.addError("Only one edited DWP response should be seen against each case, please correct");
        }
    }

    private void validateDwpEditedEvidenceBundleIfThereIsAnEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEvidenceBundleDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);
        if (!isDocumentEmpty(dwpEvidenceBundleDocument)) {
            validateDwpEditedEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);
        }
    }

    private void validateDwpEditedEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEditedEvidenceBundleDocument = getEditedDwpDocument(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);

        if (isDocumentEmpty(dwpEditedEvidenceBundleDocument)) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP evidence bundle");
        }
    }

    private void validateOneDwpEditedEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpEditedEvidenceBundleDocument = countEditedDwpDocument(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);
        if (numberOfDwpEditedEvidenceBundleDocument > 1) {
            preSubmitCallbackResponse.addError("Only one edited DWP evidence bundle should be seen against each case, please correct");
        }
    }

    private boolean isDocumentEmptyOrInvalid(Optional<DwpDocument> dwpEvidenceBundleDocument) {
        return isDocumentEmpty(dwpEvidenceBundleDocument) || documentHasNoDocumentLink(dwpEvidenceBundleDocument);
    }

    private boolean isDocumentEmpty(Optional<DwpDocument> dwpEvidenceBundleDocument) {
        return dwpEvidenceBundleDocument.isEmpty();
    }

    private boolean documentHasNoDocumentLink(Optional<DwpDocument> dwpEditedResponseDocument) {
        return dwpEditedResponseDocument.map(d -> d.getValue().getDocumentLink() == null).orElse(true);
    }

    @NotNull
    private Optional<DwpDocument> getDwpDocument(SscsCaseData sscsCaseData, DwpDocumentType dwpDocumentType) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                    .filter(dwpDocument -> dwpDocument.getValue().getDocumentType() != null)
                    .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(dwpDocumentType.getValue()))
                    .findFirst();
    }

    private long countDwpDocument(SscsCaseData sscsCaseData, DwpDocumentType dwpDocumentType) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType() != null)
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(dwpDocumentType.getValue()))
                .count();
    }

    @NotNull
    private Optional<DwpDocument> getEditedDwpDocument(SscsCaseData sscsCaseData, DwpDocumentType dwpDocumentType) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType() != null)
                .filter(dwpDocument -> dwpDocument.getValue().getEditedDocumentLink() != null)
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(dwpDocumentType.getValue()))
                .findFirst();
    }

    private long countEditedDwpDocument(SscsCaseData sscsCaseData, DwpDocumentType dwpDocumentType) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType() != null)
                .filter(dwpDocument -> dwpDocument.getValue().getEditedDocumentLink() != null)
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(dwpDocumentType.getValue()))
                .count();
    }

}
