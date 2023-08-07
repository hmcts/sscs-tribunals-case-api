package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.*;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.CTSC_CLERK;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

@Service
@Slf4j
public class CreateBundleAboutToStartEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    private final ServiceRequestExecutor serviceRequestExecutor;
    private final String bundleUrl;
    private final String bundleEnglishConfig;
    private final String bundleWelshConfig;
    private final String bundleEnglishEditedConfig;
    private final String bundleWelshEditedConfig;
    private final DwpDocumentService dwpDocumentService;
    private final BundleAudioVideoPdfService bundleAudioVideoPdfService;
    private IdamService idamService;

    @Autowired
    public CreateBundleAboutToStartEventHandler(ServiceRequestExecutor serviceRequestExecutor,
                                                DwpDocumentService dwpDocumentService,
                                                BundleAudioVideoPdfService bundleAudioVideoPdfService,
                                                @Value("${bundle.url}") String bundleUrl,
                                                @Value("${bundle.english.config}") String bundleEnglishConfig,
                                                @Value("${bundle.welsh.config}") String bundleWelshConfig,
                                                @Value("${bundle.english.edited.config}") String bundleEnglishEditedConfig,
                                                @Value("${bundle.welsh.edited.config}") String bundleWelshEditedConfig,
                                                IdamService idamService) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.dwpDocumentService = dwpDocumentService;
        this.bundleAudioVideoPdfService = bundleAudioVideoPdfService;
        this.bundleUrl = bundleUrl;
        this.bundleEnglishConfig = bundleEnglishConfig;
        this.bundleWelshConfig = bundleWelshConfig;
        this.bundleEnglishEditedConfig = bundleEnglishEditedConfig;
        this.bundleWelshEditedConfig = bundleWelshEditedConfig;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.CREATE_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        moveDocsToDwpCollectionIfOldPattern(sscsCaseData);

        if (hasMandatoryFilesMissing(sscsCaseData)) {

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            final boolean hasSuperUserRole = userDetails.hasRole(SUPER_USER);
            final boolean hasCaseWorkerRole = userDetails.hasRole(CTSC_CLERK);

            if (!hasSuperUserRole && !hasCaseWorkerRole) {
                response.addError("The bundle cannot be created as mandatory FTA documents are missing");
            } else {
                response.addWarning("The bundle cannot be created as mandatory FTA documents are missing, do you want to proceed?");
            }
        }
        return response;
    }


    private boolean hasMandatoryFilesMissing(SscsCaseData sscsCaseData) {
        boolean depResponseDocsMissing = isDwpResponseDocsMissing(sscsCaseData);
        if (!depResponseDocsMissing) {
            return isDwpEvidenceBundleDocsMissing(sscsCaseData);
        }
        return true;
    }

    private boolean isDwpEvidenceBundleDocsMissing(SscsCaseData sscsCaseData) {
        List<DwpDocument> dwpEvidenceBundleDocs = getDwpEvidenceBundleDocs(sscsCaseData);
        return dwpEvidenceBundleDocs.isEmpty() || hasMandatoryDocumentMissingForLegacyAppeals(dwpEvidenceBundleDocs);
    }

    private boolean isDwpResponseDocsMissing(SscsCaseData sscsCaseData) {
        List<DwpDocument> dwpResponseDocs = getDwpResponseDocs(sscsCaseData);
        return dwpResponseDocs.isEmpty() || hasMandatoryDocumentMissingForLegacyAppeals(dwpResponseDocs);
    }

    @NotNull
    private List<DwpDocument> getDwpEvidenceBundleDocs(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(e -> DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue().equals(e.getValue().getDocumentType()))
                .toList();
    }

    @NotNull
    private List<DwpDocument> getDwpResponseDocs(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getDwpDocuments()).stream()
                .filter(e -> DwpDocumentType.DWP_RESPONSE.getValue().equals(e.getValue().getDocumentType()))
                .toList();
    }

    private boolean hasMandatoryDocumentMissingForLegacyAppeals(List<DwpDocument> dwpResponseDocs) {
        return dwpResponseDocs.stream().anyMatch(e -> isNull(e.getValue().getDocumentLink()));
    }

    private void moveDocsToDwpCollectionIfOldPattern(SscsCaseData sscsCaseData) {
        //Before we moved to the new DWP document collection, we stored FTA documents within their own fields. This would break bundling with the new config that
        //looks at the new DWP document collection. Therefore, if the FTA fields are populated, then assume old pattern and move to the DWP document collection.
        if (sscsCaseData.getDwpResponseDocument() != null) {
            dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
        }
        if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
            dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
        }
    }
}
