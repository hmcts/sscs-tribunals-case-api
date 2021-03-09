package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToSubmitHandler.ACTIONS_THAT_REQUIRES_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isSelectedEvidence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
public class ProcessAudioVideoEvidenceMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final DocumentConfiguration documentConfiguration;

    @Autowired
    public ProcessAudioVideoEvidenceMidEventHandler(GenerateFile generateFile,
                                          DocumentConfiguration documentConfiguration) {
        this.generateFile = generateFile;
        this.documentConfiguration = documentConfiguration;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        final DynamicList processAudioVideoAction = caseData.getProcessAudioVideoAction();
        AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = caseData.getSelectedAudioVideoEvidenceDetails();

        if (nonNull(selectedAudioVideoEvidenceDetails) && nonNull(processAudioVideoAction) && ACTIONS_THAT_REQUIRES_NOTICE.contains(processAudioVideoAction.getValue().getCode())) {
            String templateId = documentConfiguration.getDocuments().get(caseData.getLanguagePreference()).get(EventType.DIRECTION_ISSUED);
            return issueDocument(callback, DocumentType.DIRECTION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        AudioVideoEvidence selectedAudioVideoEvidence = caseData.getAudioVideoEvidence().stream().filter(evidence -> isSelectedEvidence(evidence, caseData)).findFirst().orElse(null);

        if (nonNull(selectedAudioVideoEvidence)) {

            if (nonNull(selectedAudioVideoEvidence.getValue().getRip1Document())) {
                caseData.setShowRip1DocPage(YES);
            } else {
                caseData.setShowRip1DocPage(NO);
            }
            setDocumentType(selectedAudioVideoEvidence.getValue());
            selectedAudioVideoEvidenceDetails = selectedAudioVideoEvidence.getValue();
        }

        caseData.setSelectedAudioVideoEvidenceDetails(selectedAudioVideoEvidenceDetails);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void setDocumentType(AudioVideoEvidenceDetails evidence) {
        DocumentType documentType = getDocumentType(evidence);

        String documentTypeLabel = null;

        if (nonNull(documentType)) {
            documentTypeLabel = documentType.getLabel();
        }

        evidence.setDocumentType(documentTypeLabel);
    }
}
