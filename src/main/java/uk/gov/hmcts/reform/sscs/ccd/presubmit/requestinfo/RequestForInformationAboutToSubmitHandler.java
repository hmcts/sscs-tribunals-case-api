package uk.gov.hmcts.reform.sscs.ccd.presubmit.requestinfo;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class RequestForInformationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.REQUEST_FOR_INFORMATION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        log.info(String.format("Handling request info incomplete application event for caseId %s", sscsCaseData.getCcdCaseId()));

        if ("yes".equalsIgnoreCase(sscsCaseData.getResponseRequired())) {
            sscsCaseData.setInterlocReviewState(AWAITING_INFORMATION);
        }

        if (State.INCOMPLETE_APPLICATION_INFORMATION_REQUESTED.equals(caseDetails.getState())
            || State.INCOMPLETE_APPLICATION.equals(caseDetails.getState())
            || State.INTERLOCUTORY_REVIEW_STATE.equals(caseDetails.getState())) {

            log.info(String.format("Setting state to incompleteApplicationInformationRequested for caseId %s", sscsCaseData.getCcdCaseId()));
            sscsCaseData.setState(State.INCOMPLETE_APPLICATION_INFORMATION_REQUESTED);
        }

        sscsCaseData.setResponseRequired(null);

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return callbackResponse;
    }
}
