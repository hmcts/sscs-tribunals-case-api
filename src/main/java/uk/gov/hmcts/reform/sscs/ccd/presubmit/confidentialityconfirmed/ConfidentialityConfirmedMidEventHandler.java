package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;

import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
class ConfidentialityConfirmedMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmOtherPartyConfidentialityEnabled;
    private static final String MISSING_CONFIDENTIALITY_MSG = "Confidentiality for all parties must be determined to either Yes or No.";

    public ConfidentialityConfirmedMidEventHandler(@Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        if (!cmOtherPartyConfidentialityEnabled
            || callbackType != CallbackType.MID_EVENT
            || callback.getEvent() != EventType.CONFIDENTIALITY_CONFIRMED) {
            return false;
        }

        return callback.getCaseDetails().getCaseData().isBenefitType(CHILD_SUPPORT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        boolean confidentialityMissing = Optional.ofNullable(caseData.getOtherParties())
                .map(list -> list.stream()
                .filter(Objects::nonNull)
                .map(CcdValue::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.getConfidentialityRequired() == null))
                .orElse(false);

        if (confidentialityMissing) {
            preSubmitCallbackResponse.addError(MISSING_CONFIDENTIALITY_MSG);
        }

        return preSubmitCallbackResponse;
    }
}
