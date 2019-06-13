package uk.gov.hmcts.reform.sscs.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class InterlocService {
    private final Map<EventType, String> eventTypeToSecondaryStatus;

    @Autowired
    public InterlocService() {
        eventTypeToSecondaryStatus = new HashMap<>();
        eventTypeToSecondaryStatus.put(EventType.iNTERLOC_SEND_TO_TCW, "reviewByTcw");
        eventTypeToSecondaryStatus.put(EventType.TCW_DIRECTION_ISSUED, "awaitingInformation");
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_INFORMATION_RECEIVED, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_SEND_TO_JUDGE, "reviewByJudge");
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, "awaitingInformation");
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, "reviewByJudge");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.REINSTATE_APPEAL, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, null);
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, null);
    }

    public SscsCaseData setInterlocSecondaryState(EventType notificationEventType, SscsCaseData newSscsCaseData) {
        if (eventTypeToSecondaryStatus.containsKey(notificationEventType)) {
            String interlocSecondaryStatus = eventTypeToSecondaryStatus.get(notificationEventType);
            newSscsCaseData.setInterlocSecondaryState(interlocSecondaryStatus);
        }

        return newSscsCaseData;
    }
}
