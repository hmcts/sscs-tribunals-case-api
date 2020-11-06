package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

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
public class CreateBundleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;

    private String bundleUrl;
    private String bundleWelshConfig;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    @Autowired
    public CreateBundleAboutToStartHandler(ServiceRequestExecutor serviceRequestExecutor,
                                           @Value("${bundle.url}") String bundleUrl,
                                           @Value("${bundle.welsh.config}") String bundleWelshConfig) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bundleUrl = bundleUrl;
        this.bundleWelshConfig = bundleWelshConfig;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.CREATE_BUNDLE;
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
            response.addError("The bundle cannot be created as mandatory DWP documents are missing");
            return response;
        } else {

            if (sscsCaseData.getDwpResponseDocument() != null && sscsCaseData.getDwpResponseDocument().getDocumentFileName() == null) {
                sscsCaseData.getDwpResponseDocument().setDocumentFileName(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX);
            }

            if (sscsCaseData.getDwpEvidenceBundleDocument() != null && sscsCaseData.getDwpEvidenceBundleDocument().getDocumentFileName() == null) {
                sscsCaseData.getDwpEvidenceBundleDocument().setDocumentFileName(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX);
            }
            if (sscsCaseData.getSscsDocument() != null) {
                for (SscsDocument sscsDocument : sscsCaseData.getSscsDocument()) {
                    if (sscsDocument.getValue() != null && sscsDocument.getValue().getDocumentFileName() == null) {
                        sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename());
                    }
                }
            }
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleWelshConfig);
                log.info("Setting the bundleConfiguration on the case: " + bundleWelshConfig);
            }
            return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        }
    }

    private boolean checkMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDwpResponseDocument() == null
            || sscsCaseData.getDwpResponseDocument().getDocumentLink() == null
            || sscsCaseData.getDwpEvidenceBundleDocument() == null
            || sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink() == null;
    }
}
