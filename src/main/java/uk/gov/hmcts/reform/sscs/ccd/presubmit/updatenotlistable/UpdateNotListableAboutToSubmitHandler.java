package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableDirectionsFulfilled())) {
            sscsCaseData.setState(State.READY_TO_LIST);
            sscsCaseData.setNotListableProvideReasons(null);
            sscsCaseData.setDirectionDueDate(null);
        }

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableInterlocReview())) {
            InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                    .filter(x -> x.getCcdDefinition().equals(sscsCaseData.getUpdateNotListableWhoReviewsCase()))
                    .findFirst()
                    .orElse(null);
            sscsCaseData.setInterlocReviewState(interlocState);
            sscsCaseData.setInterlocReferralDate(LocalDate.now());
            sscsCaseData.setDirectionDueDate(null);
        }

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableSetNewDueDate())) {
            sscsCaseData.setDirectionDueDate(sscsCaseData.getUpdateNotListableDueDate());
        }

        if ("no".equalsIgnoreCase(sscsCaseData.getUpdateNotListableSetNewDueDate())) {
            sscsCaseData.setDirectionDueDate(null);
        }

        if (null != sscsCaseData.getUpdateNotListableWhereShouldCaseMoveTo()) {
            sscsCaseData.setState(State.getById(sscsCaseData.getUpdateNotListableWhereShouldCaseMoveTo()));
            sscsCaseData.setNotListableProvideReasons(null);
        }

        if ("yes".equalsIgnoreCase(sscsCaseData.getUpdateNotListableDirectionsFulfilled())
                || ("no".equalsIgnoreCase(sscsCaseData.getUpdateNotListableDirectionsFulfilled())
                && "no".equalsIgnoreCase(sscsCaseData.getUpdateNotListableInterlocReview())
                && "no".equalsIgnoreCase(sscsCaseData.getUpdateNotListableSetNewDueDate())
                && State.READY_TO_LIST.getId().equals(sscsCaseData.getUpdateNotListableWhereShouldCaseMoveTo()))) {
            sscsCaseData.setShouldReadyToListBeTriggered(YES);
        } else {
            sscsCaseData.setShouldReadyToListBeTriggered(null);
        }

        clearTransientFields(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
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
