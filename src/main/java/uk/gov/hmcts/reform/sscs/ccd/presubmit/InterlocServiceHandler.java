package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
@Slf4j
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
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, "awaitingInformation");
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, "reviewByJudge");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.REINSTATE_APPEAL, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.UPLOAD_FURTHER_EVIDENCE, "interlocutoryReview");
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, "none");
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, "none");
        eventTypeToSecondaryStatus.put(EventType.SEND_TO_ADMIN, "awaitingAdminAction");
        eventTypeToSecondaryStatus.put(EventType.DWP_CHALLENGE_VALIDITY, "reviewByJudge");

        return eventTypeToSecondaryStatus;
    }

    protected SscsCaseData setField(SscsCaseData newSscsCaseData, String newValue, EventType eventType) {
        log.info("Setting interloc review field to " + newValue);
        newSscsCaseData.setInterlocReviewState(newValue);

        if (eventType.equals(EventType.NON_COMPLIANT) || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)) {
            log.info("Setting interloc referral date to " + LocalDate.now().toString());
            newSscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
        }

        return newSscsCaseData;
    }
}
