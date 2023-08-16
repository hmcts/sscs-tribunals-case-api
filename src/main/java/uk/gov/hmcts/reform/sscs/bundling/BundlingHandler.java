package uk.gov.hmcts.reform.sscs.bundling;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.counting;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.util.ConfidentialityRequestUtil.isAtLeastOneRequestInProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.MultiBundleConfig;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

@Service
@Slf4j
public class BundlingHandler {

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
    public BundlingHandler(ServiceRequestExecutor serviceRequestExecutor,
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

    public PreSubmitCallbackResponse<SscsCaseData> handle(Callback<SscsCaseData> callback) {
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        moveDocsToDwpCollectionIfOldPattern(sscsCaseData);
        setDocumentFileNameIfNotSet(sscsCaseData);

        if (hasBundleAdditionsWithSameCharacter(sscsCaseData) && !callback.isIgnoreWarnings()) {
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
            response.addWarning("Some documents in this Bundle contain the same addition letter. Are you sure you want to proceed?");
            return response;
        }

        moveExistingBundlesToHistoricalBundlesThenClearExistingBundles(sscsCaseData);
        createPdfsForAnyAudioVideoEvidence(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
        setMultiBundleConfig(sscsCaseData, response);

        if (isNotEmpty(response.getErrors())) {
            log.info("Error found in bundle creation process with message \"{}\" for case id {} ", join("\", \"", response.getErrors()), callback.getCaseDetails().getId());
            return response;
        }

        log.info("Setting the bundleConfiguration on the case {} for case id {}", sscsCaseData.getBundleConfiguration(), callback.getCaseDetails().getId());

        BundleCallback<SscsCaseData> bundleCallback = new BundleCallback(callback);
        return sendToBundleService(bundleCallback);
    }

    private PreSubmitCallbackResponse<SscsCaseData> sendToBundleService(BundleCallback<SscsCaseData> callback) {
        return serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
    }

    private void createPdfsForAnyAudioVideoEvidence(SscsCaseData sscsCaseData) {
        bundleAudioVideoPdfService.createAudioVideoPdf(sscsCaseData);
    }

    private void moveExistingBundlesToHistoricalBundlesThenClearExistingBundles(SscsCaseData sscsCaseData) {
        List<Bundle> historicalBundles = new ArrayList<>();
        if (nonNull(sscsCaseData.getHistoricalBundles())) {
            historicalBundles.addAll(sscsCaseData.getHistoricalBundles());
        }
        if (nonNull(sscsCaseData.getCaseBundles())) {
            historicalBundles.addAll(sscsCaseData.getCaseBundles());
        }
        sscsCaseData.setHistoricalBundles(historicalBundles);
        sscsCaseData.setCaseBundles(null);
    }

    private void setDocumentFileNameIfNotSet(SscsCaseData sscsCaseData) {
        setDocumentFileNameOnDwpResponseDocument(sscsCaseData);
        setDocumentFileNameOnDwpEvidenceDocument(sscsCaseData);
        setDocumentFileNameOnSscsDocuments(sscsCaseData);
    }

    private boolean hasBundleAdditionsWithSameCharacter(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getSscsDocument())
                .stream()
                .filter(document -> document.getValue() != null)
                .map(document -> document.getValue().getBundleAddition())
                .filter(bundleAddition -> bundleAddition != null)
                .map(String::toUpperCase)
                .collect(Collectors.groupingBy(Function.identity(), counting()))    // create a map {A=2,B=1}
                .entrySet().stream()
                .filter(bundleAdditionItem -> bundleAdditionItem.getValue() > 1)
                .count() > 0;

    }

    private void setDocumentFileNameOnDwpResponseDocument(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getDwpDocuments())
                .stream()
                .filter(dwpDocument -> DwpDocumentType.DWP_RESPONSE.getValue().equals(dwpDocument.getValue().getDocumentType()))
                .filter(dwpDocument -> isNull(dwpDocument.getValue().getDocumentFileName()))
                .forEach(dwpDocument -> dwpDocument.getValue().setDocumentFileName(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX));
    }

    private void setDocumentFileNameOnDwpEvidenceDocument(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getDwpDocuments())
                .stream()
                .filter(dwpDocument -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(dwpDocument.getValue().getDocumentType()))
                .filter(dwpDocument -> isNull(dwpDocument.getValue().getDocumentFileName()))
                .forEach(dwpDocument -> dwpDocument.getValue().setDocumentFileName(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX));
    }

    private void setDocumentFileNameOnSscsDocuments(SscsCaseData sscsCaseData) {
        emptyIfNull(sscsCaseData.getSscsDocument())
                .stream()
                .filter(sscsDocument -> sscsDocument.getValue() != null)
                .filter(sscsDocument -> isNull(sscsDocument.getValue().getDocumentFileName()))
                .forEach(sscsDocument -> sscsDocument.getValue().setDocumentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename()));
    }

    private void setMultiBundleConfig(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        boolean hasEditedDwpResponseDocument = hasEditedDwpResponseDocument(sscsCaseData);
        boolean hasEditedDwpEvidenceBundleDocument = hasEditedDwpEvidenceBundleDocument(sscsCaseData);

        if (!hasPheRequestOrConfidentialityUnderReview(sscsCaseData, response, hasEditedDwpResponseDocument, hasEditedDwpEvidenceBundleDocument)) {

            sscsCaseData.setMultiBundleConfiguration(getMultiBundleConfigs(sscsCaseData));
        }
    }

    private boolean hasPheRequestOrConfidentialityUnderReview(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response, boolean hasEditedDwpResponseDocument, boolean hasEditedDwpEvidenceBundleDocument) {
        boolean isChildSupport = sscsCaseData.isBenefitType(Benefit.CHILD_SUPPORT);
        boolean isPheStatusUnderReview = isPheStatusUnderReview(sscsCaseData);
        boolean hasEditedResponseOrEvidenceDoc = hasEditedDwpResponseDocument || hasEditedDwpEvidenceBundleDocument;
        if (!isChildSupport && isPheStatusUnderReview && hasEditedResponseOrEvidenceDoc) {
            response.addError("There is a pending PHE request on this case");
        }

        if (isAtLeastOneRequestInProgress(sscsCaseData)) {
            response.addError("There is a pending enhanced confidentiality request on this case");
        }
        return !response.getErrors().isEmpty();
    }

    private boolean hasEditedDwpEvidenceBundleDocument(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(dwpDocument -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(dwpDocument.getValue().getDocumentType()))
                .anyMatch(dwpDocument -> nonNull(dwpDocument.getValue().getEditedDocumentLink()));
    }

    private boolean hasEditedDwpResponseDocument(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(dwpDocument -> DwpDocumentType.DWP_RESPONSE.getValue().equals(dwpDocument.getValue().getDocumentType()))
                .anyMatch(dwpDocument -> nonNull(dwpDocument.getValue().getEditedDocumentLink()));
    }

    private boolean hasEditedSscsDocuments(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getSscsDocument()).stream()
                .anyMatch(doc -> nonNull(doc.getValue().getEditedDocumentLink()));
    }

    private List<MultiBundleConfig> getMultiBundleConfigs(SscsCaseData sscsCaseData) {
        boolean requiresMultiBundleForPhe = isPheReviewGranted(sscsCaseData) && (hasEditedDwpResponseDocument(sscsCaseData) || hasEditedDwpEvidenceBundleDocument(sscsCaseData));
        boolean requiresMultiBundleForConfidentiality = isConfidentialCase(sscsCaseData) && hasEditedSscsDocuments(sscsCaseData);
        boolean requiresMultiBundleForNonPhe = sscsCaseData.isBenefitType(Benefit.CHILD_SUPPORT)
                && (hasEditedSscsDocuments(sscsCaseData) || hasEditedDwpResponseDocument(sscsCaseData) || hasEditedDwpEvidenceBundleDocument(sscsCaseData));

        if (requiresMultiBundleForPhe || requiresMultiBundleForConfidentiality || requiresMultiBundleForNonPhe) {
            return getEditedAndUneditedConfigs(sscsCaseData);
        }
        return getUneditedConfigs(sscsCaseData);
    }

    private boolean isConfidentialCase(SscsCaseData sscsCaseData) {
        return isYes(sscsCaseData.getIsConfidentialCase());
    }

    private List<MultiBundleConfig> getUneditedConfigs(SscsCaseData sscsCaseData) {
        return sscsCaseData.isLanguagePreferenceWelsh() ? singletonList(getUnEditedConfigForWelsh()) : singletonList(getUnEditedConfigForEnglish());
    }

    private List<MultiBundleConfig> getEditedAndUneditedConfigs(SscsCaseData sscsCaseData) {
        return sscsCaseData.isLanguagePreferenceWelsh() ? getEditedAndUneditedConfigForWelsh() : getEditedAndUndeditedConfigForEnglish();
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

    private void moveDocsToDwpCollectionIfOldPattern(SscsCaseData sscsCaseData) {
        //Before we moved to the new FTA document collection, we stored FTA documents within their own fields. This would break bundling with the new config that
        //looks at the new FTA document collection. Therefore, if the FTA fields are populated, then assume old pattern and move to the FTA document collection.
        if (sscsCaseData.getDwpResponseDocument() != null) {
            dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
        }
        if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
            dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
        }
    }

    protected boolean isPheStatusUnderReview(SscsCaseData sscsCaseData) {
        return isNull(sscsCaseData.getPhmeGranted());
    }

    private boolean isPheReviewGranted(SscsCaseData sscsCaseData) {
        return isYes(sscsCaseData.getPhmeGranted());
    }
}
