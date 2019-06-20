package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class InterlocServiceHandler extends EventToFieldPreSubmitCallbackHandler {

    @Autowired
    public InterlocServiceHandler() {
        super(createMappings());
    }

    private static Map<EventType, String> createMappings() {
        Map<EventType, String> eventTypeToSecondaryStatus = new HashMap<>();
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

        return eventTypeToSecondaryStatus;
    }

    protected SscsCaseData setField(SscsCaseData newSscsCaseData, String newValue) {
        newSscsCaseData.setInterlocReviewState(newValue);

        return newSscsCaseData;
    }
}
