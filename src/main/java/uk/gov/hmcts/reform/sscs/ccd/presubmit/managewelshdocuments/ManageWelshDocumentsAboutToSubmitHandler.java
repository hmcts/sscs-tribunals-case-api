package uk.gov.hmcts.reform.sscs.ccd.presubmit.managewelshdocuments;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageWelshDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.MANAGE_WELSH_DOCUMENTS);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(
            CallbackType callbackType,
            Callback<SscsCaseData> callback,
            String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);
        log.info("About to submit Manage Welsh Documents for caseID:  {}", caseData.getCcdCaseId());
        PreSubmitCallbackResponse<SscsCaseData>  preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        List<SscsWelshDocument> uploadDocuments = uploadDocuments(
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData().getSscsWelshDocuments() : null,
                caseData.getSscsWelshDocuments());

        if (uploadDocuments != null) {
            caseData.getWorkAllocationFields().setUploadedWelshDocumentTypes(uploadDocuments.stream()
                    .map(d -> d.getValue().getDocumentType())
                    .distinct().collect(Collectors.toList()));
        } else {
            caseData.getWorkAllocationFields().setUploadedWelshDocumentTypes(null);
        }

        return preSubmitCallbackResponse;
    }

    private List<SscsWelshDocument> uploadDocuments(List<SscsWelshDocument> welshDocumentsBefore, List<SscsWelshDocument> welshDocuments) {
        Map<String, Optional<String>> existingDocumentTypes = null;
        if (welshDocumentsBefore != null) {
            existingDocumentTypes = welshDocumentsBefore.stream().collect(
                    Collectors.toMap(d -> d.getId(), d -> Optional.ofNullable(d.getValue().getDocumentType())));
        }

        return uploadDocuments(existingDocumentTypes, welshDocuments);
    }

    public List<SscsWelshDocument> uploadDocuments(final Map<String, Optional<String>> existingDocumentTypes, List<SscsWelshDocument> welshDocuments) {
        if (welshDocuments != null) {
            return welshDocuments.stream()
                    .filter(d -> isNewDocumentOrTypeChanged(existingDocumentTypes, d))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private boolean isNewDocumentOrTypeChanged(Map<String, Optional<String>> existingDocumentTypes, SscsWelshDocument welshDocument) {
        if (existingDocumentTypes != null) {
            if (existingDocumentTypes.containsKey(welshDocument.getId())) {
                return !StringUtils.equals(welshDocument.getValue().getDocumentType(),
                        existingDocumentTypes.get(welshDocument.getId()).orElse(null));
            }
        }
        return true;
    }
}
