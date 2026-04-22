package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
class ConfidentialityConfirmedAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final int dwpResponseDueDaysChildSupport;
    private final boolean cmOtherPartyConfidentialityEnabled;

    public ConfidentialityConfirmedAboutToSubmitHandler(@Value("${dwp.response.due.days-child-support}") int dwpResponseDueDaysChildSupport, @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.dwpResponseDueDaysChildSupport = dwpResponseDueDaysChildSupport;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        if (!cmOtherPartyConfidentialityEnabled
            || callbackType != CallbackType.ABOUT_TO_SUBMIT
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
        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
        caseData.setDwpDueDate(LocalDate.now().plusDays(dwpResponseDueDaysChildSupport).toString());
        caseData.setDwpState(DwpState.UNREGISTERED);
        return preSubmitCallbackResponse;
    }
}
