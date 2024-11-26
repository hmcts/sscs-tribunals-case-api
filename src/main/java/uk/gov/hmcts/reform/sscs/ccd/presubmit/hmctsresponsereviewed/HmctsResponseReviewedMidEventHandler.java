package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

public class HmctsResponseReviewedMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isIbcCase();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getIssueCodeIbcaOnly() != null && "DD".equals(sscsCaseData.getIssueCodeIbcaOnly())) {
            response.addError("You cannot proceed with DD as the case Issue code. Please select another issue code before proceeding.");
        }

        return response;
    }
}
