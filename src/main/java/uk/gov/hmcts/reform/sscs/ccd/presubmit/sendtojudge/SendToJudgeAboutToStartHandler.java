package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtojudge;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class SendToJudgeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final boolean postHearingsB;

    public SendToJudgeAboutToStartHandler(@Value("${feature.postHearingsB.enabled}")  boolean postHearingsB) {
        this.postHearingsB = postHearingsB;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && (callback.getEvent() == EventType.TCW_REFER_TO_JUDGE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        if (postHearingsB) {
            sscsCaseData.setPrePostHearing(null);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
