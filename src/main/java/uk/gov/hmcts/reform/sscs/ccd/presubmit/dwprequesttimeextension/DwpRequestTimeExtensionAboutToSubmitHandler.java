package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static java.util.Objects.requireNonNull;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DwpRequestTimeExtensionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return CallbackType.ABOUT_TO_SUBMIT == callbackType
            && EventType.DWP_REQUEST_TIME_EXTENSION == callback.getEvent();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.EXTENSION_REQUESTED.getId());
        callback.getCaseDetails().getCaseData().setInterlocReferralReason(InterlocReferralReason.TIME_EXTENSION.getId());
        callback.getCaseDetails().getCaseData().setTimeExtensionRequested("Yes");
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }
}
