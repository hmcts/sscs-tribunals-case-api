package uk.gov.hmcts.reform.sscs.ccd.presubmit.attachscanneddocs;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class AttachScannedDocsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.deleted-redacted-doc.enabled}")
    private boolean deletedRedactedDocEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ATTACH_SCANNED_DOCS
                && callback.getCaseDetails() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.setEvidenceHandled(NO.getValue());
        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);

        if (deletedRedactedDocEnabled) {
            var scannedDocuments = callback.getCaseDetails().getCaseData().getScannedDocuments();

            if (scannedDocuments != null) {
                callback.getCaseDetailsBefore().ifPresent(
                        sscsCaseDataCaseDetailsBefore -> {
                            var scannedDocumentsBefore = sscsCaseDataCaseDetailsBefore.getCaseData().getScannedDocuments();

                            if (scannedDocumentsBefore != null) {
                                scannedDocumentsBefore.stream()
                                        .filter(scannedDocumentBefore -> scannedDocumentBefore.getValue().getEditedUrl() != null)
                                        .forEach(scannedDocumentBefore -> {
                                            scannedDocuments
                                                    .stream()
                                                    .forEach(scannedDocument -> {
                                                        if (compare(scannedDocumentBefore, scannedDocument)) {
                                                            scannedDocument.getValue().setEditedUrl(scannedDocumentBefore.getValue().getEditedUrl());
                                                        }
                                                    });
                                        });

                            }
                        });
            } else {
                log.error("There are no scanned documents in this case");
            }
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private boolean compare(ScannedDocument doc1, ScannedDocument doc2) {
        return doc1.getValue().getUrl().getDocumentUrl().equals(doc2.getValue().getUrl().getDocumentUrl());
    }

}
