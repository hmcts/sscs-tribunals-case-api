package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;

import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUndetermined;
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

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return caseData.isBenefitType(CHILD_SUPPORT) || caseData.isBenefitType(UC);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        boolean otherPartyConfidentialityMissing = Optional.ofNullable(caseData.getOtherParties())
                .map(list -> list.stream()
                .filter(Objects::nonNull)
                .map(CcdValue::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.getConfidentialityRequirement() == null
                        || value.getConfidentialityRequirement() == YesNoUndetermined.UNDETERMINED))
                .orElse(false);

        boolean appellantConfidentialityMissing = Optional.ofNullable(caseData.getAppeal())
            .map(Appeal::getAppellant)
            .map(appellant -> appellant.getConfidentialityRequirement() == null
                    || appellant.getConfidentialityRequirement() == YesNoUndetermined.UNDETERMINED)
            .orElse(false);

        if (otherPartyConfidentialityMissing || appellantConfidentialityMissing) {
            preSubmitCallbackResponse.addError(MISSING_CONFIDENTIALITY_MSG);
        }

        return preSubmitCallbackResponse;
    }
}
