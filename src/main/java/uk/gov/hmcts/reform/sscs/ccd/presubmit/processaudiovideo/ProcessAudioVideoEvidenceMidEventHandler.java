package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToSubmitHandler.ACTIONS_THAT_REQUIRES_NOTICE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
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

        if (nonNull(processAudioVideoAction) && ACTIONS_THAT_REQUIRES_NOTICE.contains(processAudioVideoAction.getValue().getCode())) {
            String templateId = documentConfiguration.getDocuments().get(caseData.getLanguagePreference()).get(EventType.DIRECTION_ISSUED);
            return issueDocument(callback, DocumentType.DIRECTION_NOTICE, templateId, generateFile, userAuthorisation);
        }
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
