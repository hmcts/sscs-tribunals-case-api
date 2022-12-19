package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import java.util.Arrays;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class FeNoActionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.FE_NO_ACTION)
            && DwpState.FE_RECEIVED.equals(callback.getCaseDetails().getCaseData().getDwpState());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        DwpState dwpState = Arrays.stream(DwpState.values())
            .filter(x -> x.getCcdDefinition().equals(caseData.getDwpStateFeNoAction().getValue().getCode()))
            .findFirst()
            .orElse(null);
        caseData.setDwpState(dwpState);
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }
}
