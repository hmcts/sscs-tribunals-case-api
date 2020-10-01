package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ReviewConfidentialityRequestMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
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

        if (sscsCaseData.getConfidentialityRequestOutcomeJointParty() != null && RequestOutcome.NOT_SET.equals(sscsCaseData.getConfidentialityRequestOutcomeJointParty().getRequestOutcome())) {
            sscsCaseData.setConfidentialityRequestOutcomeJointParty(null);
        }
        if (sscsCaseData.getConfidentialityRequestOutcomeAppellant() != null && RequestOutcome.NOT_SET.equals(sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome())) {
            sscsCaseData.setConfidentialityRequestOutcomeAppellant(null);
        }

        return preSubmitCallbackResponse;
    }

   
}
