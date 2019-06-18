package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class InterlocServiceHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final Map<EventType, String> eventTypeToSecondaryStatus;

    @Autowired
    public InterlocServiceHandler() {
        eventTypeToSecondaryStatus = new HashMap<>();
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_SEND_TO_TCW, "reviewByTcw");
        eventTypeToSecondaryStatus.put(EventType.TCW_DIRECTION_ISSUED, "awaitingInformation");
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_INFORMATION_RECEIVED, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_SENT_TO_JUDGE, "reviewByJudge");
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, "awaitingInformation");
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, "reviewByJudge");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.REINSTATE_APPEAL, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, null);
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, null);
    }

    public boolean canHandle(Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return eventTypeToSecondaryStatus.containsKey(callback.getEvent());
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(Callback<SscsCaseData> callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        final SscsCaseData updatedInterlocCaseData = setInterlocReviewState(callback.getEvent(), sscsCaseData);

        return new PreSubmitCallbackResponse<>(updatedInterlocCaseData);
    }

    private SscsCaseData setInterlocReviewState(EventType notificationEventType, SscsCaseData newSscsCaseData) {
        String interlocSecondaryStatus = eventTypeToSecondaryStatus.get(notificationEventType);
        newSscsCaseData.setInterlocReviewState(interlocSecondaryStatus);

        return newSscsCaseData;
    }
}
