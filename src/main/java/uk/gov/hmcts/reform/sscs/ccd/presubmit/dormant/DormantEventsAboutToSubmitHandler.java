package uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class DormantEventsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == EventType.HMCTS_LAPSE_CASE
                || callback.getEvent() == EventType.CONFIRM_LAPSED
                || callback.getEvent() == EventType.WITHDRAWN
                || callback.getEvent() == EventType.LAPSED_REVISED
                || callback.getEvent() == EventType.DORMANT
                || callback.getEvent() == EventType.ADMIN_SEND_TO_DORMANT_APPEAL_STATE
                || callback.getEvent() == EventType.ADMIN_APPEAL_WITHDRAWN
                || callback.getEvent() == EventType.ISSUE_FINAL_DECISION
            );
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Handling {} event for case id {}", callback.getEvent(), callback.getCaseDetails().getId());

        caseData.setInterlocReviewState(null);
        caseData.setDirectionDueDate(null);
        callback.getCaseDetailsBefore().ifPresent(f -> caseData.setPreviousState(f.getState()));

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
