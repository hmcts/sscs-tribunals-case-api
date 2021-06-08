package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

@Component
@Slf4j
public class ManageDwpDocumentsAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

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

        return validateDwpDocuments(sscsCaseData);
    }

    private PreSubmitCallbackResponse<SscsCaseData> validateDwpDocuments(SscsCaseData sscsCaseData) {
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        validateDwpResponseDocument(sscsCaseData, preSubmitCallbackResponse);
        validateDwpEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);

        if (sscsCaseData.getDwpEditedEvidenceReason() != null) {
            validateEditedResponseAndBundle(sscsCaseData, preSubmitCallbackResponse);
        }
        return preSubmitCallbackResponse;
    }

    private void validateDwpResponseDocument(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpResponseDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_RESPONSE);
        if (isDocumentEmptyOrInvalid(dwpResponseDocument)) {
            preSubmitCallbackResponse.addError("DWP response document cannot be empty");
        }
    }

    private void validateDwpEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEvidenceBundleDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);
        if (isDocumentEmptyOrInvalid(dwpEvidenceBundleDocument)) {
            preSubmitCallbackResponse.addError("DWP evidence bundle cannot be empty");
        }
    }

    private void validateEditedResponseAndBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        validateDwpEditedResponse(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpEditedResponse(sscsCaseData, preSubmitCallbackResponse);

        validateDwpEditedEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);
        validateOneDwpEditedEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);
    }

    private void validateDwpEditedResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEditedResponseDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_EDITED_RESPONSE);

        if (isDocumentEmptyOrInvalid(dwpEditedResponseDocument)) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP response document");
        }
    }

    private void validateOneDwpEditedResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpEditedResponses = countDwpDocument(sscsCaseData, DwpDocumentType.DWP_EDITED_RESPONSE);
        if (numberOfDwpEditedResponses > 1) {
            preSubmitCallbackResponse.addError("Only one DWP response should be seen against each case, please correct");
        }
    }

    private void validateDwpEditedEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Optional<DwpDocument> dwpEditedEvidenceBundleDocument = getDwpDocument(sscsCaseData, DwpDocumentType.DWP_EDITED_EVIDENCE_BUNDLE);

        if (isDocumentEmptyOrInvalid(dwpEditedEvidenceBundleDocument)) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP evidence bundle");
        }
    }

    private void validateOneDwpEditedEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        final long numberOfDwpEditedEvidenceBundleDocument = countDwpDocument(sscsCaseData, DwpDocumentType.DWP_EDITED_EVIDENCE_BUNDLE);
        if (numberOfDwpEditedEvidenceBundleDocument > 1) {
            preSubmitCallbackResponse.addError("Only one DWP evidence bundle should be seen against each case, please correct");
        }
    }

    private boolean isDocumentEmptyOrInvalid(Optional<DwpDocument> dwpEvidenceBundleDocument) {
        return dwpEvidenceBundleDocument.isEmpty() || documentHasNoDocumentLink(dwpEvidenceBundleDocument);
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


}
