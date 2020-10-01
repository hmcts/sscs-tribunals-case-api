package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ReviewConfidentialityRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
            && callback.getEvent() == EventType.REVIEW_CONFIDENTIALITY_REQUEST
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
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

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!isAtLeastOneRequestInProgress(sscsCaseData)) {

            preSubmitCallbackResponse.addError("There is no confidentiality request to review");
            return preSubmitCallbackResponse;
        } else {
            if (sscsCaseData.getConfidentialityRequestOutcomeAppellant() == null) {
                sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.NOT_SET).build());
            }
            if (sscsCaseData.getConfidentialityRequestOutcomeJointParty() == null) {
                sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.NOT_SET).build());
            }
            return preSubmitCallbackResponse;
        }
    }
}

