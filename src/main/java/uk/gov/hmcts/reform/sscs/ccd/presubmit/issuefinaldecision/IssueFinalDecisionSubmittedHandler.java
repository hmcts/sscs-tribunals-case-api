package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

@Component
@Slf4j
@AllArgsConstructor
class IssueFinalDecisionSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.SUBMITTED
            && callback.getEvent() == EventType.ISSUE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        Correction correction = sscsCaseData.getPostHearing().getCorrection();
        if (isPostHearingEnabled && isYes(correction.getCorrectionFinalDecisionInProgress())) {
            correction.setCorrectionFinalDecisionInProgress(YesNo.NO);
            sscsCaseData = ccdCallbackMapService.handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData);
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
