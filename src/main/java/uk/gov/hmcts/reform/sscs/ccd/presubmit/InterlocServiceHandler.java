package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.INTERLOCUTORY_REVIEW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_TCW;

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
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_SEND_TO_TCW, REVIEW_BY_TCW.getId());
        eventTypeToSecondaryStatus.put(EventType.TCW_DIRECTION_ISSUED, AWAITING_INFORMATION.getId());
        eventTypeToSecondaryStatus.put(EventType.INTERLOC_INFORMATION_RECEIVED, INTERLOCUTORY_REVIEW.getId());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, AWAITING_INFORMATION.getId());
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, REVIEW_BY_JUDGE.getId());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, INTERLOCUTORY_REVIEW.getId());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, INTERLOCUTORY_REVIEW.getId());
        eventTypeToSecondaryStatus.put(EventType.REINSTATE_APPEAL, INTERLOCUTORY_REVIEW.getId());
        eventTypeToSecondaryStatus.put(EventType.UPLOAD_FURTHER_EVIDENCE, INTERLOCUTORY_REVIEW.getId());
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, NONE.getId());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, NONE.getId());
        eventTypeToSecondaryStatus.put(EventType.SEND_TO_ADMIN, AWAITING_ADMIN_ACTION.getId());
        eventTypeToSecondaryStatus.put(EventType.DWP_CHALLENGE_VALIDITY, "reviewByJudge");
        return eventTypeToSecondaryStatus;
    }

    protected SscsCaseData setField(SscsCaseData newSscsCaseData, String newValue, EventType eventType) {
        log.info("Case({}): Setting interloc review field to {}", newSscsCaseData.getCcdCaseId(), newValue);
        newSscsCaseData.setInterlocReviewState(newValue);

        if (eventType.equals(EventType.NON_COMPLIANT) || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)) {
            log.info("Setting interloc referral date to " + LocalDate.now().toString());
            newSscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
        }

        return newSscsCaseData;
    }
}
