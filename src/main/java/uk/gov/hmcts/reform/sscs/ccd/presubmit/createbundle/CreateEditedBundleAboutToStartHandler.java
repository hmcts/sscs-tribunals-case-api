package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;


@Service
@Slf4j
public class CreateEditedBundleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;

    private String bundleUrl;
    private String bundleWelshConfig;
    private String bundleEditedConfig;
    private String bundleWelshEditedConfig;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    @Autowired
    public CreateEditedBundleAboutToStartHandler(ServiceRequestExecutor serviceRequestExecutor,
                                                 @Value("${bundle.url}") String bundleUrl,
                                                 @Value("${bundle.welsh.config}") String bundleWelshConfig,
                                                 @Value("${bundle.edited.config}") String bundleEditedConfig,
                                                 @Value("${bundle.welsh.edited.config}") String bundleWelshEditedConfig) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bundleUrl = bundleUrl;
        this.bundleWelshConfig = bundleWelshConfig;
        this.bundleEditedConfig = bundleEditedConfig;
        this.bundleWelshEditedConfig = bundleWelshEditedConfig;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.CREATE_EDITED_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (checkMandatoryFilesMissing(sscsCaseData)) {
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(
                callback.getCaseDetails().getCaseData());
            response.addError("The edited bundle cannot be created as mandatory edited DWP documents are missing");
            return response;
        } else {
            if (sscsCaseData.getDwpEditedResponseDocument() != null && sscsCaseData.getDwpEditedResponseDocument().getDocumentFileName() == null) {
                sscsCaseData.getDwpEditedResponseDocument().setDocumentFileName(DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX);
            }

            if (sscsCaseData.getDwpEditedEvidenceBundleDocument() != null && sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentFileName() == null) {
                sscsCaseData.getDwpEditedEvidenceBundleDocument().setDocumentFileName(DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX);
            }

            if (sscsCaseData.getSscsDocument() != null) {
                for (SscsDocument sscsDocument : sscsCaseData.getSscsDocument()) {
                    if (sscsDocument.getValue() != null && sscsDocument.getValue().getDocumentFileName() == null) {
                        sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename());
                    }
                }
            }

            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleWelshEditedConfig);
                log.info("Setting the editedBundleConfiguration {} on the case: {}",
                        bundleWelshEditedConfig, sscsCaseData.getCcdCaseId());
            } else {
                sscsCaseData.setBundleConfiguration(bundleEditedConfig);
            }

            return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        }
    }

    private boolean checkMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDwpEditedResponseDocument() == null
            || sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() == null
            || sscsCaseData.getDwpEditedEvidenceBundleDocument() == null
            || sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink() == null;
    }
}
