package uk.gov.hmcts.reform.sscs.ccd.presubmit.deathofappellant;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class DeathOfAppellantAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.DEATH_OF_APPELLANT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        log.info("Setting interloc review state on appeal after recording death of an appellant for case id: {}", callback.getCaseDetails().getId());

        preSubmitCallbackResponse.getData().setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
        preSubmitCallbackResponse.getData().setDwpUcb(null);

        if (null != preSubmitCallbackResponse.getData().getSubscriptions()
            && null != preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription()) {
            preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription().setSubscribeEmail("No");
            preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription().setSubscribeSms("No");
        }

        return preSubmitCallbackResponse;
    }
}
