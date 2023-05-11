package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class IssueAdjournmentNoticeSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.SUBMITTED
            && callback.getEvent() == EventType.ISSUE_ADJOURNMENT_NOTICE
            && isAdjournmentEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        SscsUtil.clearAdjournmentTransientFields(caseData, isAdjournmentEnabled);

        return new PreSubmitCallbackResponse<>(caseData);
    }

}
