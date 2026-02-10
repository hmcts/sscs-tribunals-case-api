package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class AddOtherPartyMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmOtherPartyConfidentialityEnabled;

    public AddOtherPartyMidEventHandler(@Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        if (!cmOtherPartyConfidentialityEnabled
            || callbackType != CallbackType.MID_EVENT
            || callback.getEvent() != EventType.ADD_OTHER_PARTY_DATA
            || isNull(callback.getCaseDetails().getCaseData().getOtherParties())) {
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

        if (caseData.getOtherParties() == null || caseData.getOtherParties().isEmpty()) {
            log.debug("Other party must be added to submit this event. ccdCaseId: {}", caseData.getCcdCaseId());
            preSubmitCallbackResponse.addError("Other party must be added to submit this event.");
        }

        if (caseData.getOtherParties().size() > 1) {
            log.debug("Only one other party can be added using this event. ccdCaseId: {}",
                caseData.getCcdCaseId());
            preSubmitCallbackResponse.addError("Only one other party can be added using this event.");
        }

        return preSubmitCallbackResponse;
    }
}
