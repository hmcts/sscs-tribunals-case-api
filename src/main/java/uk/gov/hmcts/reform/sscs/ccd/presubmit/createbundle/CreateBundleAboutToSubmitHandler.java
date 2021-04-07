package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

@Service
@Slf4j
public class CreateBundleAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private ServiceRequestExecutor serviceRequestExecutor;

    private String bundleUrl;

    private boolean multiBundleFeature;
    private String bundleEnglishConfig;
    private String bundleWelshConfig;
    private String bundleEditedConfig;
    private String bundleWelshEditedConfig;
    private String bundleUnEditedConfig;
    private String bundleWelshUnEditedConfig;

    private static String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    private DwpDocumentService dwpDocumentService;

    private final BundleAudioVideoPdfService bundleAudioVideoPdfService;

    @Autowired
    public CreateBundleAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                            DwpDocumentService dwpDocumentService,
                                            BundleAudioVideoPdfService bundleAudioVideoPdfService,
                                            @Value("${feature.multi-bundle-feature.enabled}") boolean multiBundleFeature,
                                            @Value("${bundle.url}") String bundleUrl,
                                            @Value("${bundle.english.config}") String bundleEnglishConfig,
                                            @Value("${bundle.welsh.config}") String bundleWelshConfig,
                                            @Value("${bundle.edited.config}") String bundleEditedConfig,
                                            @Value("${bundle.welsh.edited.config}") String bundleWelshEditedConfig,
                                            @Value("${bundle.unedited.config}") String bundleUnEditedConfig,
                                            @Value("${bundle.welsh.unedited.config}") String bundleWelshUnEditedConfig) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.dwpDocumentService = dwpDocumentService;
        this.bundleAudioVideoPdfService = bundleAudioVideoPdfService;
        this.multiBundleFeature = multiBundleFeature;
        this.bundleUrl = bundleUrl;
        this.bundleEnglishConfig = bundleEnglishConfig;
        this.bundleWelshConfig = bundleWelshConfig;
        this.bundleEditedConfig = bundleEditedConfig;
        this.bundleWelshEditedConfig = bundleWelshEditedConfig;
        this.bundleUnEditedConfig = bundleUnEditedConfig;
        this.bundleWelshUnEditedConfig = bundleWelshUnEditedConfig;
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

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        if (checkMandatoryFilesMissing(sscsCaseData)) {
            response.addError("The bundle cannot be created as mandatory DWP documents are missing");
            return response;
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

            if (sscsCaseData.getSscsDocument() != null) {
                for (SscsDocument sscsDocument : sscsCaseData.getSscsDocument()) {
                    if (sscsDocument.getValue() != null && sscsDocument.getValue().getDocumentFileName() == null) {
                        sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename());
                    }
                }
            }

            callback.getCaseDetails().getCaseData().setCaseBundles(null);

            bundleAudioVideoPdfService.createAudioVideoPdf(sscsCaseData);

            if (multiBundleFeature) {
                setMultiBundleConfig(sscsCaseData, response);
            } else {
                setBundleConfig(sscsCaseData);
            }

            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                log.info("Error found in bundle creation process for case id {}", callback.getCaseDetails().getId());
                return response;
            } else {
                log.info("Setting the bundleConfiguration on the case {} for case id {}", sscsCaseData.getBundleConfiguration(), callback.getCaseDetails().getId());

                return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
            }
        }
    }

    private void setBundleConfig(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getDwpDocuments().stream().filter(f -> (f.getValue().getDocumentType().equals(DwpDocumentType.DWP_RESPONSE.getValue())
                && f.getValue().getEditedDocumentLink() != null)
                || f.getValue().getDocumentType().equals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())
                && f.getValue().getEditedDocumentLink() != null).count() > 0) {
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleWelshUnEditedConfig);
            } else {
                sscsCaseData.setBundleConfiguration(bundleUnEditedConfig);
            }
        } else {
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                sscsCaseData.setBundleConfiguration(bundleWelshConfig);
            } else {
                sscsCaseData.setBundleConfiguration(bundleEnglishConfig);
            }
        }
    }

    private void setMultiBundleConfig(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        List<MultiBundleConfig> configs = new ArrayList<>();
        
        if (sscsCaseData.getDwpDocuments().stream().filter(f -> (f.getValue().getDocumentType().equals(DwpDocumentType.DWP_RESPONSE.getValue())
                && f.getValue().getEditedDocumentLink() != null)
                || f.getValue().getDocumentType().equals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())
                && f.getValue().getEditedDocumentLink() != null).count() > 0 &&  checkPhmeReviewIsGrantedOrUnderReview(sscsCaseData))  {

            if (checkPhmeStatusIsUnderReview(sscsCaseData)) {
                response.addError("There is a pending PHME request on this case");
                return;
            }

            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                configs.add(MultiBundleConfig.builder().value(bundleWelshEditedConfig).build());
                configs.add(MultiBundleConfig.builder().value(bundleWelshUnEditedConfig).build());
            } else {
                configs.add(MultiBundleConfig.builder().value(bundleEditedConfig).build());
                configs.add(MultiBundleConfig.builder().value(bundleUnEditedConfig).build());
            }

        } else {
            if (sscsCaseData.isLanguagePreferenceWelsh()) {
                configs.add(MultiBundleConfig.builder().value(bundleWelshConfig).build());
            } else {
                configs.add(MultiBundleConfig.builder().value(bundleEnglishConfig).build());
            }
        }
        sscsCaseData.setMultiBundleConfiguration(configs);
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
        //Before we moved to the new DWP document collection, we stored DWP documents within their own fields. This would break bundling with the new config that
        //looks at the new DWP document collection. Therefore, if the DWP fields are populated, then assume old pattern and move to the DWP document collection.
        if (sscsCaseData.getDwpResponseDocument() != null) {
            dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
        }
        if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
            dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
        }
    }

    protected boolean checkPhmeStatusIsUnderReview(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPhmeGranted() == null;
    }

    private boolean checkPhmeReviewIsGrantedOrUnderReview(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPhmeGranted() == null || sscsCaseData.getPhmeGranted().equals(YesNo.YES);
    }

}
