package uk.gov.hmcts.reform.sscs.ccd.presubmit.editbundle;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.BundleRequestExecutor;

@Service
public class EditBundleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private BundleRequestExecutor bundleRequestExecutor;
    private String bundleUrl;

    private static String EDIT_BUNDLE_ENDPOINT = "/api/stitch-ccd-bundles";

    @Autowired
    public EditBundleAboutToStartHandler(BundleRequestExecutor bundleRequestExecutor,
                                         @Value("${bundle.url}") String bundleUrl) {
        this.bundleRequestExecutor = bundleRequestExecutor;
        this.bundleUrl = bundleUrl;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.EDIT_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (sscsCaseData.getCaseBundles() != null) {
            boolean eligibleForStitching = false;
            for (Bundle bundle : sscsCaseData.getCaseBundles()) {
                if ("Yes".equals(bundle.getValue().getEligibleForStitching())) {
                    eligibleForStitching = true;
                    bundle.getValue().setFileName("SscsBundle.pdf");
                    bundle.getValue().setCoverpageTemplate("SSCS-cover-page.docx");
                    bundle.getValue().setHasTableOfContents("Yes");
                    bundle.getValue().setHasCoversheets("Yes");
                    bundle.getValue().setPaginationStyle("topCenter");
                    bundle.getValue().setPageNumberFormat("numberOfPages");
                    bundle.getValue().setStitchedDocument(null);
                }
            }
            if (!eligibleForStitching && !callback.isIgnoreWarnings()) {
                PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
                preSubmitCallbackResponse.addWarning("No bundle selected to be amended. The stitched PDF will not be updated. Are you sure you want to continue?");
                return preSubmitCallbackResponse;
            } else {
                return bundleRequestExecutor.post(callback, bundleUrl + EDIT_BUNDLE_ENDPOINT);
            }
        } else {
            return null;
        }

    }

}
