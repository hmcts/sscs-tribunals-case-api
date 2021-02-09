package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;


@Service
@Slf4j
public class CreateEditedBundleAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;

    private String bundleUrl;
    private String bundleEditedConfig;
    private String bundleWelshEditedConfig;

    //FIXME to be remove after dwpDocumentsBundleFeature turned on
    private String bundleNewEditedConfig;
    private String bundleNewWelshEditedConfig;
    private boolean dwpDocumentsBundleFeature;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    @Autowired
    public CreateEditedBundleAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                                  @Value("${bundle.url}") String bundleUrl,
                                                  @Value("${bundle.edited.config}") String bundleEditedConfig,
                                                  @Value("${bundle.welsh.edited.config}") String bundleWelshEditedConfig,
                                                  @Value("${bundle.new.edited.config}") String bundleNewEditedConfig,
                                                  @Value("${bundle.new.welsh.edited.config}") String bundleNewWelshEditedConfig,
                                                  @Value("${feature.dwp-documents-bundle.enabled}") boolean dwpDocumentsBundleFeature) {

        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bundleUrl = bundleUrl;
        this.bundleEditedConfig = bundleEditedConfig;
        this.bundleWelshEditedConfig = bundleWelshEditedConfig;
        this.bundleNewEditedConfig = bundleNewEditedConfig;
        this.bundleNewWelshEditedConfig = bundleNewWelshEditedConfig;
        this.dwpDocumentsBundleFeature = dwpDocumentsBundleFeature;
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

        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(
                callback.getCaseDetails().getCaseData());

        if (dwpDocumentsBundleFeature) {
            if ((checkMandatoryFilesMissing(sscsCaseData))) {
                errorResponse.addError("The edited bundle cannot be created as mandatory edited DWP documents are missing");
            }
        } else {
            if ((checkMandatoryFilesMissingOld(sscsCaseData))) {
                errorResponse.addError("The edited bundle cannot be created as mandatory edited DWP documents are missing");
            }
        }

        if (checkPhmeStatusIsNotGranted(sscsCaseData)) {
            errorResponse.addError("The edited bundle cannot be created as PHME status has not been granted");
        }

        if (errorResponse.getErrors() != null && errorResponse.getErrors().size() > 0) {
            return errorResponse;
        } else {
            if (sscsCaseData.getSscsDocument() != null) {
                for (SscsDocument sscsDocument : sscsCaseData.getSscsDocument()) {
                    if (sscsDocument.getValue() != null && sscsDocument.getValue().getDocumentFileName() == null) {
                        sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename());
                    }
                }
            }

            if (dwpDocumentsBundleFeature) {
                //FIXME to be removed after dwpDocumentsBundleFeature turned on
                if (sscsCaseData.isLanguagePreferenceWelsh()) {
                    sscsCaseData.setBundleConfiguration(bundleNewWelshEditedConfig);
                } else {
                    sscsCaseData.setBundleConfiguration(bundleNewEditedConfig);
                }
            } else {
                if (sscsCaseData.isLanguagePreferenceWelsh()) {
                    sscsCaseData.setBundleConfiguration(bundleWelshEditedConfig);
                } else {
                    sscsCaseData.setBundleConfiguration(bundleEditedConfig);
                }
            }

            log.info("Setting the edited bundleConfiguration on the case {} for case id {}", sscsCaseData.getBundleConfiguration(), callback.getCaseDetails().getId());

            return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        }
    }

    protected boolean checkPhmeStatusIsNotGranted(SscsCaseData sscsCaseData) {

        return sscsCaseData.getPhmeGranted() == null || sscsCaseData.getPhmeGranted().getValue().equals("No");
    }

    private boolean checkMandatoryFilesMissingOld(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDwpEditedResponseDocument() == null
                || sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() == null
                || sscsCaseData.getDwpEditedEvidenceBundleDocument() == null
                || sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink() == null;
    }

    private boolean checkMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        if (null != sscsCaseData.getDwpDocuments()) {

            List<DwpDocument> dwpEditedResponseDocs = sscsCaseData.getDwpDocuments().stream().filter(e -> DwpDocumentType.DWP_RESPONSE.getValue().equals(e.getValue().getDocumentType()) && null != e.getValue().getEditedDocumentLink()).collect(toList());
            List<DwpDocument> dwpEditedEvidenceBundleDocs = sscsCaseData.getDwpDocuments().stream().filter(e -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(e.getValue().getDocumentType()) && null != e.getValue().getEditedDocumentLink()).collect(toList());

            if (dwpEditedResponseDocs.size() == 0 || dwpEditedResponseDocs.stream().filter(e -> null == e.getValue().getDocumentLink()).count() > 0) {
                return true;
            }

            if (dwpEditedEvidenceBundleDocs.size() == 0 || dwpEditedEvidenceBundleDocs.stream().filter(e -> null == e.getValue().getDocumentLink()).count() > 0) {
                return true;
            }
            return false;
        }
        return true;
    }
}
