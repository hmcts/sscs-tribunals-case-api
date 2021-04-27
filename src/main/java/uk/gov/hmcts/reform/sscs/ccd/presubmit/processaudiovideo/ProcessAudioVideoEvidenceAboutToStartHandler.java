package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class ProcessAudioVideoEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;

    @Autowired
    public ProcessAudioVideoEvidenceAboutToStartHandler(IdamService idamService) {
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (sscsCaseData.getAudioVideoEvidence() == null || sscsCaseData.getAudioVideoEvidence().isEmpty()) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            errorResponse.addError("Before running this event audio and video evidence must be uploaded");
            return errorResponse;
        }
        ProcessAudioVideoActionHelper.setSelectedAudioVideoEvidence(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
