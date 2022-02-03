package uk.gov.hmcts.reform.sscs.ccd.presubmit.processreasonableadjustment;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

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
public class ProcessReasonableAdjustmentAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.PROCESS_REASONABLE_ADJUSTMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if ((sscsCaseData.getReasonableAdjustmentsLetters() == null)
                || (isEmpty(sscsCaseData.getReasonableAdjustmentsLetters().getAppellant())
                && isEmpty(sscsCaseData.getReasonableAdjustmentsLetters().getRepresentative())
                && isEmpty(sscsCaseData.getReasonableAdjustmentsLetters().getAppointee())
                && isEmpty(sscsCaseData.getReasonableAdjustmentsLetters().getJointParty())
                && isEmpty(sscsCaseData.getReasonableAdjustmentsLetters().getOtherParty()))) {
            callbackResponse.addError("No reasonable adjustment correspondence has been generated on this case");
        }

        sscsCaseData.updateReasonableAdjustmentsOutstanding();

        return callbackResponse;
    }

}
