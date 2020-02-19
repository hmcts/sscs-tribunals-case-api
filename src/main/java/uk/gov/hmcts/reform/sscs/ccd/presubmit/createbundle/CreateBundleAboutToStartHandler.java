package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.BundleRequestExecutor;

@Service
public class CreateBundleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private BundleRequestExecutor bundleRequestExecutor;

    private String bundleUrl;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    @Autowired
    public CreateBundleAboutToStartHandler(BundleRequestExecutor bundleRequestExecutor,
                                           @Value("${bundle.url}") String bundleUrl) {
        this.bundleRequestExecutor = bundleRequestExecutor;
        this.bundleUrl = bundleUrl;
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

        return bundleRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
    }

}
