package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionremade;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionRemadeSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.DECISION_REMADE
                && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = ccdCallbackMapService.handleCcdCallbackMap(uk.gov.hmcts.reform.sscs.ccd.domain.UpperTribunalDecision.UPPER_TRIBUNAL_DECISION, callback.getCaseDetails().getCaseData());
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
