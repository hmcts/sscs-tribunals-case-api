package uk.gov.hmcts.reform.sscs.ccd.presubmit.subscriptionupdated;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.utility.AppealNumberGenerator.generateAppealNumber;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class SubscriptionUpdatedAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.SUBSCRIPTION_UPDATED);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        log.info("Updating subscription for case id {}", sscsCaseData.getCcdCaseId());

        Subscription appellantSubscription = sscsCaseData.getSubscriptions().getAppellantSubscription();

        if (appellantSubscription != null && !appellantSubscription.isEmpty()) {
            appellantSubscription.setTya(getTyaNumber(appellantSubscription));
        }

        Subscription appointeeSubscription = sscsCaseData.getSubscriptions().getAppointeeSubscription();

        if (appointeeSubscription != null && !appointeeSubscription.isEmpty()) {
            appointeeSubscription.setTya(getTyaNumber(appointeeSubscription));
        }

        Subscription repSubscription = sscsCaseData.getSubscriptions().getRepresentativeSubscription();

        if (repSubscription != null && !repSubscription.isEmpty()) {
            repSubscription.setTya(getTyaNumber(repSubscription));
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private String getTyaNumber(Subscription existingSubscription) {
        if (StringUtils.isBlank(existingSubscription.getTya())) {
            return generateAppealNumber();
        } else {
            return existingSubscription.getTya();
        }
    }
}
