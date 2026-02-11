package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.sethearingroute;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
@Slf4j
public class SetHearingRouteMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String ERROR_MESSAGE = "The hearing route must be set to List assist on an IBC case";

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.SET_HEARING_ROUTE
            && callback.getCaseDetails().getCaseData().isIbcCase();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        HearingRoute selectedHearingRoute = caseData.getSchedulingAndListingFields().getHearingRoute();

        if (HearingRoute.GAPS.equals(selectedHearingRoute)) {
            preSubmitCallbackResponse.addError(ERROR_MESSAGE);
        }
        return preSubmitCallbackResponse;
    }
}
