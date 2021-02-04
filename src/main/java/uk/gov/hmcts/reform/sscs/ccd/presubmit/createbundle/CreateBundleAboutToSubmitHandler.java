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
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@Service
@Slf4j
public class CreateBundleAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;

    private String bundleUrl;

    private String bundleEnglishConfig;
    private String bundleWelshConfig;
    private String bundleUnEditedConfig;
    private String bundleWelshUnEditedConfig;

    //FIXME to be remove after dwpDocumentsBundleFeature turned on
    private String bundleNewEnglishConfig;
    private String bundleNewWelshConfig;
    private String bundleNewUnEditedConfig;
    private String bundleNewWelshUnEditedConfig;

    protected boolean dwpDocumentsBundleFeature;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    private DwpDocumentService dwpDocumentService;

    @Autowired
    public CreateBundleAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                            DwpDocumentService dwpDocumentService,
                                            @Value("${bundle.url}") String bundleUrl,
                                            @Value("${bundle.english.config}") String bundleEnglishConfig,
                                            @Value("${bundle.welsh.config}") String bundleWelshConfig,
                                            @Value("${bundle.unedited.config}") String bundleUnEditedConfig,
                                            @Value("${bundle.welsh.unedited.config}") String bundleWelshUnEditedConfig,
                                            @Value("${bundle.new.english.config}") String bundleNewEnglishConfig,
                                            @Value("${bundle.new.welsh.config}") String bundleNewWelshConfig,
                                            @Value("${bundle.new.unedited.config}") String bundleNewUnEditedConfig,
                                            @Value("${bundle.new.welsh.unedited.config}") String bundleNewWelshUnEditedConfig,
                                            @Value("${feature.dwp-documents-bundle.enabled}") boolean dwpDocumentsBundleFeature) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.dwpDocumentService = dwpDocumentService;
        this.bundleUrl = bundleUrl;
        this.bundleEnglishConfig = bundleEnglishConfig;
        this.bundleWelshConfig = bundleWelshConfig;
        this.bundleUnEditedConfig = bundleUnEditedConfig;
        this.bundleWelshUnEditedConfig = bundleWelshUnEditedConfig;
        this.bundleNewEnglishConfig = bundleNewEnglishConfig;
        this.bundleNewWelshConfig = bundleNewWelshConfig;
        this.bundleNewUnEditedConfig = bundleNewUnEditedConfig;
        this.bundleNewWelshUnEditedConfig = bundleNewWelshUnEditedConfig;
        this.dwpDocumentsBundleFeature = dwpDocumentsBundleFeature;
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

        moveDocsToDwpCollectionIfOldPattern(sscsCaseData);

        if ((!dwpDocumentsBundleFeature && checkMandatoryFilesMissingOld(sscsCaseData))
                || (dwpDocumentsBundleFeature && checkMandatoryFilesMissing(sscsCaseData))) {
            PreSubmitCallbackResponse<SscsCaseData> response;
            response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
            response.addError("The bundle cannot be created as mandatory DWP documents are missing");
            return response;
        } else {

            if (!dwpDocumentsBundleFeature) {
                //FIXME: Remove this after dwpDocumentsBundleFeature switched on
                if (sscsCaseData.getDwpResponseDocument() != null && sscsCaseData.getDwpResponseDocument().getDocumentFileName() == null) {
                    sscsCaseData.getDwpResponseDocument().setDocumentFileName(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX);
                }
                if (sscsCaseData.getDwpEvidenceBundleDocument() != null && sscsCaseData.getDwpEvidenceBundleDocument().getDocumentFileName() == null) {
                    sscsCaseData.getDwpEvidenceBundleDocument().setDocumentFileName(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX);
                }
            } else {
                sscsCaseData.getDwpDocuments().forEach(f -> {
                    if (DwpDocumentType.DWP_RESPONSE.getValue().equals(f.getValue().getDocumentType()) && null == f.getValue().getDocumentFileName()) {
                        f.getValue().setDocumentFileName(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX);
                    }
                });

                sscsCaseData.getDwpDocuments().forEach(f -> {
                    if (DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(f.getValue().getDocumentType()) && null == f.getValue().getDocumentFileName()) {
                        f.getValue().setDocumentFileName(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX);
                    }
                });
            }

            if (sscsCaseData.getSscsDocument() != null) {
                for (SscsDocument sscsDocument : sscsCaseData.getSscsDocument()) {
                    if (sscsDocument.getValue() != null && sscsDocument.getValue().getDocumentFileName() == null) {
                        sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename());
                    }
                }
            }

            if (!dwpDocumentsBundleFeature) {
                //FIXME: Remove this after dwpDocumentsBundleFeature switched on
                if (sscsCaseData.isLanguagePreferenceWelsh()) {
                    if (sscsCaseData.getDwpEditedResponseDocument() != null && sscsCaseData.getDwpEditedEvidenceBundleDocument() != null) {
                        sscsCaseData.setBundleConfiguration(bundleWelshUnEditedConfig);
                    } else {
                        sscsCaseData.setBundleConfiguration(bundleWelshConfig);
                    }
                } else if (sscsCaseData.getDwpEditedResponseDocument() != null && sscsCaseData.getDwpEditedEvidenceBundleDocument() != null) {
                    sscsCaseData.setBundleConfiguration(bundleUnEditedConfig);
                } else {
                    sscsCaseData.setBundleConfiguration(bundleEnglishConfig);
                }
            } else {
                setBundleConfig(sscsCaseData);
            }

            log.info("Setting the bundleConfiguration on the case {} for case id {}", sscsCaseData.getBundleConfiguration(), callback.getCaseDetails().getId());

            return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        }
    }

    private void setBundleConfig(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getDwpDocuments().stream().filter(f -> (f.getValue().getDocumentType().equals(DwpDocumentType.DWP_RESPONSE.getValue())
                && f.getValue().getEditedDocumentLink() != null)
                || f.getValue().getDocumentType().equals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())
                && f.getValue().getEditedDocumentLink() != null).count() > 0) {
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleNewWelshUnEditedConfig);
            } else {
                sscsCaseData.setBundleConfiguration(bundleNewUnEditedConfig);
            }
        } else {
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleNewWelshConfig);
            } else {
                sscsCaseData.setBundleConfiguration(bundleNewEnglishConfig);
            }
        }
    }

    //FIXME: Remove after dwpDocumentsBundleFeature switched on
    private boolean checkMandatoryFilesMissingOld(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDwpResponseDocument() == null
                || sscsCaseData.getDwpResponseDocument().getDocumentLink() == null
                || sscsCaseData.getDwpEvidenceBundleDocument() == null
                || sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink() == null;
    }

    private boolean checkMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        if (null != sscsCaseData.getDwpDocuments()) {

            List<DwpDocument> dwpResponseDocs = sscsCaseData.getDwpDocuments().stream().filter(e -> DwpDocumentType.DWP_RESPONSE.getValue().equals(e.getValue().getDocumentType())).collect(toList());
            List<DwpDocument> dwpEvidenceBundleDocs = sscsCaseData.getDwpDocuments().stream().filter(e -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(e.getValue().getDocumentType())).collect(toList());

            if (dwpResponseDocs.size() == 0 || dwpResponseDocs.stream().filter(e -> null == e.getValue().getDocumentLink()).count() > 0) {
                return true;
            }

            if (dwpEvidenceBundleDocs.size() == 0 || dwpEvidenceBundleDocs.stream().filter(e -> null == e.getValue().getDocumentLink()).count() > 0) {
                return true;
            }
            return false;
        }
        return true;
    }


    private void moveDocsToDwpCollectionIfOldPattern(SscsCaseData sscsCaseData) {
        if (dwpDocumentsBundleFeature) {
            //Before we moved to the new DWP document collection, we stored DWP documents within their own fields. This would break bundling with the new config that
            //looks at the new DWP document collection. Therefore, if the DWP fields are populated, then assume old pattern and move to the DWP document collection.
            if (sscsCaseData.getDwpResponseDocument() != null) {
                dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
            }
            if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
                dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
            }
        }
    }
}
