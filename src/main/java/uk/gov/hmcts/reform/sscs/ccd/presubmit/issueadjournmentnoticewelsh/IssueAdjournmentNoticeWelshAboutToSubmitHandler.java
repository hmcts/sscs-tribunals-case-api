package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournmentnoticewelsh;

import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class IssueAdjournmentNoticeWelshAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_ADJOURNMENT_NOTICE_WELSH
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!sscsCaseData.isLanguagePreferenceWelsh()) {
            preSubmitCallbackResponse.addError("Error: This action is only available for Welsh cases.");
        }
        sscsCaseData.setInterlocReviewState(NONE);
        sscsCaseData.updateTranslationWorkOutstandingFlag();

        return preSubmitCallbackResponse;
    }


}
