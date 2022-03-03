package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

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

        if (isYes(sscsCaseData.getUpdateNotListableDirectionsFulfilled())) {
            sscsCaseData.setState(State.READY_TO_LIST);
            sscsCaseData.setNotListableProvideReasons(null);
            sscsCaseData.setDirectionDueDate(null);
        }

        if (isYes(sscsCaseData.getUpdateNotListableInterlocReview())) {
            sscsCaseData.setInterlocReviewState(sscsCaseData.getUpdateNotListableWhoReviewsCase());
            sscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
            sscsCaseData.setDirectionDueDate(null);
        }

        if (isYes(sscsCaseData.getUpdateNotListableSetNewDueDate())) {
            sscsCaseData.setDirectionDueDate(sscsCaseData.getUpdateNotListableDueDate());
        }

        if (isNoOrNull(sscsCaseData.getUpdateNotListableSetNewDueDate())) {
            sscsCaseData.setDirectionDueDate(null);
        }

        if (null != sscsCaseData.getUpdateNotListableWhereShouldCaseMoveTo()) {
            sscsCaseData.setState(State.getById(sscsCaseData.getUpdateNotListableWhereShouldCaseMoveTo()));
            sscsCaseData.setNotListableProvideReasons(null);
        }

        clearTransientFields(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setUpdateNotListableDirectionsFulfilled(null);
        sscsCaseData.setUpdateNotListableInterlocReview(null);
        sscsCaseData.setUpdateNotListableWhoReviewsCase(null);
        sscsCaseData.setUpdateNotListableSetNewDueDate(null);
        sscsCaseData.setUpdateNotListableDueDate(null);
        sscsCaseData.setUpdateNotListableWhereShouldCaseMoveTo(null);
    }
}
