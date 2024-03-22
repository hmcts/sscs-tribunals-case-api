package uk.gov.hmcts.reform.sscs.ccd.presubmit.managewelshdocuments;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class ManageWelshDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final AddedDocumentsUtil addedDocumentsUtil;

    private final boolean workAllocationFeature;

    public ManageWelshDocumentsAboutToSubmitHandler(AddedDocumentsUtil addedDocumentsUtil, @Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.addedDocumentsUtil = addedDocumentsUtil;
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.MANAGE_WELSH_DOCUMENTS)
                && workAllocationFeature;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(
            CallbackType callbackType,
            Callback<SscsCaseData> callback,
            String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        log.info("About to submit Manage Welsh Documents for caseID:  {}", caseData.getCcdCaseId());
        PreSubmitCallbackResponse<SscsCaseData>  preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        caseData.getWorkAllocationFields().setUploadedWelshDocumentTypes(addedDocumentsUtil.addedDocumentTypes(
                previousWelshDocuments(callback.getCaseDetailsBefore()),
                caseData.getSscsWelshDocuments()
        ));

        return preSubmitCallbackResponse;
    }


    private List<? extends AbstractDocument> previousWelshDocuments(Optional<CaseDetails<SscsCaseData>> caseData) {
        return caseData.isPresent() ? caseData.get().getCaseData().getSscsWelshDocuments() : null;
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
