package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class UpdateNotListableAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.UPDATE_NOT_LISTABLE
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

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableDirectionsFulfilled())) {
            sscsCaseData.setState(State.READY_TO_LIST);
            sscsCaseData.setNotListableProvideReasons(null);
        }

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableInterlocReview())) {
            sscsCaseData.setInterlocReviewState(sscsCaseData.getUpdateNotListableWhoReviewsCase());
            sscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
            sscsCaseData.setDirectionDueDate(null);
        }

        return preSubmitCallbackResponse;
    }
}
