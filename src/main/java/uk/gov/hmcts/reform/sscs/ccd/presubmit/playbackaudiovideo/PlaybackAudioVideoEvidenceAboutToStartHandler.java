package uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo.PlaybackAudioVideoActionHelper.getApprovedAudioVideoDocuments;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo.PlaybackAudioVideoActionHelper.setSelectedAudioVideoEvidence;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class PlaybackAudioVideoEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.PLAYBACK_AUDIO_VIDEO_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        List<? extends AbstractDocument> sscsAvDocuments = getApprovedAudioVideoDocuments(sscsCaseData.getSscsDocument());
        List<? extends AbstractDocument> dwpAvDocuments = getApprovedAudioVideoDocuments(sscsCaseData.getDwpDocuments());

        if ((sscsAvDocuments == null || sscsAvDocuments.isEmpty())
                && (dwpAvDocuments == null || dwpAvDocuments.isEmpty())) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            errorResponse.addError("Before running this event audio and video evidence must be uploaded and approved");
            return errorResponse;
        }

        List<AbstractDocument> combinedAvDocuments = new ArrayList<>(sscsAvDocuments);
        combinedAvDocuments.addAll(dwpAvDocuments);

        setSelectedAudioVideoEvidence(sscsCaseData, combinedAvDocuments);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
