package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

import java.util.List;
import java.util.Objects;

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

    private static final String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    private final ServiceRequestExecutor serviceRequestExecutor;
    private final String bundleUrl;
    private final String bundleEnglishConfig;
    private final String bundleWelshConfig;
    private final String bundleEnglishEditedConfig;
    private final String bundleWelshEditedConfig;
    private final DwpDocumentService dwpDocumentService;
    private final BundleAudioVideoPdfService bundleAudioVideoPdfService;

    @Autowired
    public CreateBundleAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                            DwpDocumentService dwpDocumentService,
                                            BundleAudioVideoPdfService bundleAudioVideoPdfService,
                                            @Value("${bundle.url}") String bundleUrl,
                                            @Value("${bundle.english.config}") String bundleEnglishConfig,
                                            @Value("${bundle.welsh.config}") String bundleWelshConfig,
                                            @Value("${bundle.english.edited.config}") String bundleEnglishEditedConfig,
                                            @Value("${bundle.welsh.edited.config}") String bundleWelshEditedConfig) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.dwpDocumentService = dwpDocumentService;
        this.bundleAudioVideoPdfService = bundleAudioVideoPdfService;
        this.bundleUrl = bundleUrl;
        this.bundleEnglishConfig = bundleEnglishConfig;
        this.bundleWelshConfig = bundleWelshConfig;
        this.bundleEnglishEditedConfig = bundleEnglishEditedConfig;
        this.bundleWelshEditedConfig = bundleWelshEditedConfig;
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

            setDocumentFileNameIfNotSet(sscsCaseData);

            clearExistingBundles(callback);

            createPdfsForAnyAudioVideoEvidence(sscsCaseData);

            setMultiBundleConfig(sscsCaseData, response);

            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                log.info("Error found in bundle creation process for case id {}", callback.getCaseDetails().getId());
                return response;
            }

            log.info("Setting the bundleConfiguration on the case {} for case id {}", sscsCaseData.getBundleConfiguration(), callback.getCaseDetails().getId());
            return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        }
    }

    private void createPdfsForAnyAudioVideoEvidence(SscsCaseData sscsCaseData) {
        bundleAudioVideoPdfService.createAudioVideoPdf(sscsCaseData);
    }

    private void clearExistingBundles(Callback<SscsCaseData> callback) {
        callback.getCaseDetails().getCaseData().setCaseBundles(null);
    }

    private void setDocumentFileNameIfNotSet(SscsCaseData sscsCaseData) {
        setDocumentFileNameOnDwpResponseDocument(sscsCaseData);
        setDocumentFileNameOnDwpEvidenceDocument(sscsCaseData);
        setDocumentFileNameOnSscsDocuments(sscsCaseData);
    }

    private void setDocumentFileNameOnDwpResponseDocument(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getDwpDocuments())
                .stream()
                .filter(f -> DwpDocumentType.DWP_RESPONSE.getValue().equals(f.getValue().getDocumentType()))
                .filter(f -> isNull(f.getValue().getDocumentFileName()))
                .forEach(f-> f.getValue().setDocumentFileName(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX));
    }

    private void setDocumentFileNameOnDwpEvidenceDocument(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getDwpDocuments())
                .stream()
                .filter(f -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(f.getValue().getDocumentType()))
                .filter(f -> isNull(f.getValue().getDocumentFileName()))
                .forEach( f-> f.getValue().setDocumentFileName(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX));
    }

    private void setDocumentFileNameOnSscsDocuments(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getSscsDocument())
                .stream()
                .filter(s-> s.getValue() != null)
                .filter(s -> isNull(s.getValue().getDocumentFileName()))
                .forEach(s -> s.getValue().setDocumentFileName(s.getValue().getDocumentLink().getDocumentFilename()));
    }

    private void setMultiBundleConfig(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        boolean hasEditedDwpResponseDocument = emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(f -> DwpDocumentType.DWP_RESPONSE.getValue().equals(f.getValue().getDocumentType()))
                .anyMatch(f -> nonNull(f.getValue().getEditedDocumentLink()));

        boolean hasEditedDwpEvidenceBundleDocument = emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(f -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(f.getValue().getDocumentType()))
                .anyMatch(f -> nonNull(f.getValue().getEditedDocumentLink()));

        if ((hasEditedDwpResponseDocument || hasEditedDwpEvidenceBundleDocument) && checkPhmeStatusIsUnderReview(sscsCaseData)) {
            response.addError("There is a pending PHME request on this case");
            return;
        }
        final List<MultiBundleConfig> configs;
        if ((hasEditedDwpResponseDocument || hasEditedDwpEvidenceBundleDocument) && checkPhmeReviewIsGranted(sscsCaseData))  {
            configs = getEditedAndUneditedConfigs(sscsCaseData);
        } else {
            configs = getUneditedConfigs(sscsCaseData);
        }
        sscsCaseData.setMultiBundleConfiguration(configs);
    }

    private List<MultiBundleConfig> getUneditedConfigs(SscsCaseData sscsCaseData) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            return singletonList(getUnEditedConfigForWelsh());
        }
        return singletonList(getUnEditedConfigForEnglish());
    }

    private List<MultiBundleConfig> getEditedAndUneditedConfigs(SscsCaseData sscsCaseData) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            return getEditedAndUneditedConfigForWelsh();
        }
        return getEditedAndUndeditedConfigForEnglish();
    }

    private List<MultiBundleConfig> getEditedAndUndeditedConfigForEnglish() {
        return asList(getMultiBundleConfig(bundleEnglishEditedConfig), getUnEditedConfigForEnglish());
    }

    private List<MultiBundleConfig> getEditedAndUneditedConfigForWelsh() {
        return asList(getMultiBundleConfig(bundleWelshEditedConfig), getUnEditedConfigForWelsh());
    }

    private MultiBundleConfig getUnEditedConfigForWelsh() {
        return getMultiBundleConfig(bundleWelshConfig);
    }

    private MultiBundleConfig getUnEditedConfigForEnglish() {
        return getMultiBundleConfig(bundleEnglishConfig);
    }

    private MultiBundleConfig getMultiBundleConfig(String config) {
        return MultiBundleConfig.builder().value(config).build();
    }

    private boolean checkMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        List<DwpDocument> dwpResponseDocs = emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(e -> DwpDocumentType.DWP_RESPONSE.getValue().equals(e.getValue().getDocumentType()))
                .collect(toList());

        if (dwpResponseDocs.isEmpty() || hasMandatoryDocumentMissingForLegacyAppeals(dwpResponseDocs)) {
            return true;
        }

        List<DwpDocument> dwpEvidenceBundleDocs = emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(e -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(e.getValue().getDocumentType()))
                .collect(toList());

        return dwpEvidenceBundleDocs.isEmpty() || hasMandatoryDocumentMissingForLegacyAppeals(dwpEvidenceBundleDocs);
    }

    private boolean hasMandatoryDocumentMissingForLegacyAppeals(List<DwpDocument> dwpResponseDocs) {
        return dwpResponseDocs.stream().anyMatch(e -> isNull(e.getValue().getDocumentLink()));
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
        return isNull(sscsCaseData.getPhmeGranted());
    }

    private boolean checkPhmeReviewIsGranted(SscsCaseData sscsCaseData) {
        return YesNo.YES.equals(sscsCaseData.getPhmeGranted());
    }

}
