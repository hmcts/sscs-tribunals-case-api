package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
@Slf4j
public class ManageDwpDocumentsAboutToStartHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DwpDocumentService dwpDocumentService;

    @Autowired
    public ManageDwpDocumentsAboutToStartHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.MANAGE_DWP_DOCUMENTS;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        migrateDwpDocumentsToTheDwpDocumentCollection(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void migrateDwpDocumentsToTheDwpDocumentCollection(SscsCaseData sscsCaseData) {
        final List<DwpDocument> dwpDocumentsToKeepAfterMigration = getDwpDocumentsToKeepAfterMigration(sscsCaseData);

        migrateToDwpDocumentsIfTheyExistInTheOldLocations(sscsCaseData);

        addBackDwpDocumentsIfTheyWereRemovedFromTheMigration(sscsCaseData, dwpDocumentsToKeepAfterMigration);
    }

    private void migrateToDwpDocumentsIfTheyExistInTheOldLocations(SscsCaseData sscsCaseData) {
        dwpDocumentService.moveDocsToCorrectCollection(sscsCaseData);
    }

    private List<DwpDocument> getDwpDocumentsToKeepAfterMigration(SscsCaseData sscsCaseData) {
        final List<DwpDocument> dwpResponseDocument = keepDwpDocumentIfRemovedFromTheMigrationToTheDwpDocumentsCollection(DWP_RESPONSE, sscsCaseData.getDwpResponseDocument(), sscsCaseData.getDwpDocuments());
        final List<DwpDocument> dwpEvidenceBundle = keepDwpDocumentIfRemovedFromTheMigrationToTheDwpDocumentsCollection(DWP_EVIDENCE_BUNDLE, sscsCaseData.getDwpEvidenceBundleDocument(), sscsCaseData.getDwpDocuments());
        return Stream.of(dwpEvidenceBundle, dwpResponseDocument).flatMap(Collection::stream).toList();
    }

    private void addBackDwpDocumentsIfTheyWereRemovedFromTheMigration(SscsCaseData sscsCaseData, Collection<DwpDocument> dwpDocumentsToKeepAfterMigration) {
        if (isNotEmpty(dwpDocumentsToKeepAfterMigration)) {
            sscsCaseData.getDwpDocuments().addAll(dwpDocumentsToKeepAfterMigration);
        }
    }

    @NotNull
    private List<DwpDocument> keepDwpDocumentIfRemovedFromTheMigrationToTheDwpDocumentsCollection(DwpDocumentType dwpDocumentType, DwpResponseDocument dwpResponseDocument, List<DwpDocument> dwpDocuments) {
        return emptyIfNull(dwpDocuments).stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(dwpDocumentType.getValue()))
                .filter(doc -> dwpResponseDocument != null)
                .filter(doc -> dwpResponseDocument.getDocumentLink() != null)
                .toList();
    }
}
