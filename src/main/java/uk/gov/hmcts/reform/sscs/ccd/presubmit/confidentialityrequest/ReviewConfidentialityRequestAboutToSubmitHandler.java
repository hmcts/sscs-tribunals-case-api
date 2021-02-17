package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.CONFIDENTIALITY_ACTION_REQUIRED;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ReviewConfidentialityRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.REVIEW_CONFIDENTIALITY_REQUEST
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

        try {

            if (isAtLeastOneRequestInProgress(sscsCaseData)) {

                validatePartySubmissionFieldsAreValid("Appellant",
                    isAppellantRequestInProgress(sscsCaseData), sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());

                validatePartySubmissionFieldsAreValid("Joint Party",
                    isJointPartyRequestInProgress(sscsCaseData), sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

                // Set the request status on Appellant and Joint Party and return whether either
                // status update represents a granting of confidentiality now.
                boolean appellantGrantedNow = processAppellantAndReturnWhetherGrantedNow(sscsCaseData);
                boolean jointPartyGrantedNow = processJointPartyAndReturnWhetherGrantedNow(sscsCaseData);

                if (appellantGrantedNow || jointPartyGrantedNow) {
                    if (!State.RESPONSE_RECEIVED.equals(callback.getCaseDetails().getState())) {
                        sscsCaseData.setDwpState(CONFIDENTIALITY_ACTION_REQUIRED.getId());
                        sscsCaseData.setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION.getId());
                        sscsCaseData.setState(State.NOT_LISTABLE);
                    } else {
                        sscsCaseData.setDwpState(null);
                        sscsCaseData.setInterlocReviewState(null);
                    }
                    sscsCaseData.setIsConfidentialCase(YesNo.YES);
                    sscsCaseData.setIsProgressingViaGaps(YesNo.YES.getValue());
                    log.info("'Confidentiality - Action Required' set on case id " + sscsCaseData.getCcdCaseId());
                } else {
                    sscsCaseData.setInterlocReviewState(InterlocReviewState.NONE.getId());
                }

                clearTransientFields(preSubmitCallbackResponse);

            } else {
                throw new IllegalStateException("There is no confidentiality request to review");
            }

        } catch (IllegalStateException e) {
            preSubmitCallbackResponse.addError(e.getMessage() + ". Please check case data. If problem continues please contact support");
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
        }

        return preSubmitCallbackResponse;
    }

    private void validatePartySubmissionFieldsAreValid(String partyName, boolean requestInProgress, String grantedOrRefusedText) {
        if (requestInProgress) {
            if (!isValidPopulatedGrantedOrRefusedValue(grantedOrRefusedText)) {
                throw new IllegalStateException(partyName + " confidentiality request is in progress but value set for granted or refused is:" + grantedOrRefusedText);
            }
        } else {
            if (isPopulatedGrantedOrRefusedValue(grantedOrRefusedText)) {
                throw new IllegalStateException(partyName + " confidentiality request is not in progress but value set for granted or refused is:" + grantedOrRefusedText);
            }
        }
    }

    private boolean isValidPopulatedGrantedOrRefusedValue(String value) {
        return "grantConfidentialityRequest".equals(value) || "refuseConfidentialityRequest".equals(value);
    }

    private boolean isPopulatedGrantedOrRefusedValue(String value) {
        return value != null;
    }

    private void clearTransientFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(null);
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(null);
    }

    private boolean processAPartyAndReturnWhetherGrantedNow(SscsCaseData sscsCaseData,
        String grantedOrRefusedText,
        Consumer<DatedRequestOutcome> setOutcomeCallback, String partyName) {

        boolean grantedNow = false;

        if ("grantConfidentialityRequest".equals(grantedOrRefusedText)) {
            setOutcome(setOutcomeCallback, RequestOutcome.GRANTED);
            log.info("'Confidentiality Granted for " + partyName + " for case id " + sscsCaseData.getCcdCaseId());
            grantedNow = true;
        } else if ("refuseConfidentialityRequest".equals(grantedOrRefusedText)) {
            log.info("'Confidentiality Refused for " + partyName + " for case id " + sscsCaseData.getCcdCaseId());
            setOutcome(setOutcomeCallback, RequestOutcome.REFUSED);
        }
        return grantedNow;
    }

    private void setOutcome(Consumer<DatedRequestOutcome> setOutcomeCallback, RequestOutcome outcome) {
        setOutcomeCallback.accept(DatedRequestOutcome.builder().requestOutcome(outcome).date(LocalDate.now()).build());
    }

    private boolean processJointPartyAndReturnWhetherGrantedNow(SscsCaseData sscsCaseData) {
        return processAPartyAndReturnWhetherGrantedNow(sscsCaseData,
            sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused(),
            sscsCaseData::setConfidentialityRequestOutcomeJointParty,
            "Joint Party");
    }

    private boolean processAppellantAndReturnWhetherGrantedNow(SscsCaseData sscsCaseData) {
        return processAPartyAndReturnWhetherGrantedNow(sscsCaseData,
            sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused(),
            sscsCaseData::setConfidentialityRequestOutcomeAppellant,
            "Appellant");
    }

    private boolean isAtLeastOneRequestInProgress(SscsCaseData sscsCaseData) {
        return isAppellantRequestInProgress(sscsCaseData)
            || isJointPartyRequestInProgress(sscsCaseData);
    }

    private boolean isAppellantRequestInProgress(SscsCaseData sscsCaseData) {
        return RequestOutcome.IN_PROGRESS
            .equals(getRequestOutcome(sscsCaseData.getConfidentialityRequestOutcomeAppellant()));
    }

    private boolean isJointPartyRequestInProgress(SscsCaseData sscsCaseData) {
        return RequestOutcome.IN_PROGRESS
            .equals(getRequestOutcome(sscsCaseData.getConfidentialityRequestOutcomeJointParty()));
    }

    private RequestOutcome getRequestOutcome(DatedRequestOutcome datedRequestOutcome) {
        return datedRequestOutcome == null ? null : datedRequestOutcome.getRequestOutcome();
    }
}
