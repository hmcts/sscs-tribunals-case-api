package uk.gov.hmcts.reform.sscs.ccd.presubmit.editbundle;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;


@Service
@Slf4j
public class EditBundleAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;
    private String bundleUrl;
    private final DocumentConfiguration documentConfiguration;

    private static String EDIT_BUNDLE_ENDPOINT = "/api/stitch-ccd-bundles";

    @Autowired
    public EditBundleAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                          @Value("${bundle.url}") String bundleUrl,
                                          DocumentConfiguration documentConfiguration) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bundleUrl = bundleUrl;
        this.documentConfiguration = documentConfiguration;
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
        final String template = documentConfiguration.getCover().get(sscsCaseData.getLanguagePreference());

        log.info("EditBundleAboutToStartHandler getLanguagePreference  {}", sscsCaseData.getLanguagePreference());
        log.info("EditBundleAboutToStartHandler Coversheet Template {}", template);

        if (sscsCaseData.getCaseBundles() != null) {
            boolean eligibleForStitching = false;
            for (Bundle bundle : sscsCaseData.getCaseBundles()) {
                if ("Yes".equals(bundle.getValue().getEligibleForStitching())) {

                    final String bundleName = null != bundle.getValue().getStitchedDocument()
                            ? bundle.getValue().getStitchedDocument().getDocumentFilename() : sscsCaseData.getCcdCaseId() + "-SscsBundle";

                    log.info("EditBundleAboutToStartHandler Bundle File Name {}", bundleName);

                    eligibleForStitching = true;
                    bundle.getValue().setFileName(bundleName);
                    bundle.getValue().setCoverpageTemplate(template);
                    bundle.getValue().setHasTableOfContents("Yes");
                    bundle.getValue().setHasCoversheets("Yes");
                    bundle.getValue().setPageNumberFormat("numberOfPages");
                    bundle.getValue().setStitchedDocument(null);
                }
            }

            return processRequest(eligibleForStitching, callback, sscsCaseData);

        } else {
            return null;

        }
    }

    public PreSubmitCallbackResponse<SscsCaseData> processRequest(boolean eligibleForStitching, Callback<SscsCaseData> callback, SscsCaseData sscsCaseData) {
        if (!eligibleForStitching && !callback.isIgnoreWarnings()) {

            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addWarning("No bundle selected to be amended. The stitched PDF will not be updated. Are you sure you want to continue?");

            return preSubmitCallbackResponse;
        } else {
            return serviceRequestExecutor.post(callback, bundleUrl + EDIT_BUNDLE_ENDPOINT);
        }
    }
}
