package uk.gov.hmcts.reform.sscs.ccd.presubmit.deathofappellant;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.APPOINTEE_DETAILS_NEEDED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome.GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;

import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class DeathOfAppellantAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final Validator validator;

    protected DeathOfAppellantAboutToSubmitHandler(Validator validator) {
        this.validator = validator;
    }

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

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        log.info("Setting interloc review state on appeal after recording death of an appellant for case id: {}", callback.getCaseDetails().getId());

        preSubmitCallbackResponse.getData().setDwpUcb(null);

        if (null != preSubmitCallbackResponse.getData().getSubscriptions()
            && null != preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription()) {
            preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription().setSubscribeEmail("No");
            preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription().setSubscribeSms("No");
            preSubmitCallbackResponse.getData().getSubscriptions().getAppellantSubscription().setWantSmsNotifications("No");
        }


        Optional<CaseDetails<SscsCaseData>> beforeOptional = callback.getCaseDetailsBefore();
        CaseDetails<SscsCaseData> caseDataAfter = callback.getCaseDetails();
        CaseDetails<SscsCaseData> caseDataBefore = null;

        if (beforeOptional.isPresent()) {
            caseDataBefore = beforeOptional.get();
        }

        Appointee appointeeBefore = null;

        if (caseDataBefore != null && caseDataBefore.getCaseData().getAppeal().getAppellant().getAppointee() != null) {
            appointeeBefore = caseDataBefore.getCaseData().getAppeal().getAppellant().getAppointee();
        }

        Appointee appointeeAfter = caseDataAfter.getCaseData().getAppeal().getAppellant().getAppointee();

        if (shouldSetInterlocReviewState(appointeeBefore, appointeeAfter)) {
            preSubmitCallbackResponse.getData().setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
        }

        if ((appointeeBefore == null || "no".equalsIgnoreCase(caseDataBefore.getCaseData().getAppeal().getAppellant().getIsAppointee()) || null == caseDataBefore.getCaseData().getAppeal().getAppellant().getIsAppointee())
                && appointeeAfter == null || "no".equalsIgnoreCase(caseDataAfter.getCaseData().getAppeal().getAppellant().getIsAppointee()) || null == caseDataAfter.getCaseData().getAppeal().getAppellant().getIsAppointee()) {
            preSubmitCallbackResponse.getData().setDwpState(APPOINTEE_DETAILS_NEEDED.getId());
        }

        preSubmitCallbackResponse.getData().setConfidentialityRequestOutcomeAppellant(null);

        if (!shouldKeepConfidentialCaseFlag(caseDataAfter)) {
            preSubmitCallbackResponse.getData().setIsConfidentialCase(null);
        }

        return preSubmitCallbackResponse;
    }

    private boolean shouldSetInterlocReviewState(Appointee appointeeBefore, Appointee appointeeAfter) {

        return !(null != appointeeBefore && null != appointeeAfter && appointeeBefore.equals(appointeeAfter));
    }

    private boolean shouldKeepConfidentialCaseFlag(CaseDetails<SscsCaseData> caseData) {

        return null != caseData.getCaseData().getConfidentialityRequestOutcomeJointParty()
            && null != caseData.getCaseData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome()
            && GRANTED.equals(caseData.getCaseData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
    }

}
