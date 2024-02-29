package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
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
        eventTypeToSecondaryStatus.put(EventType.TCW_DIRECTION_ISSUED, AWAITING_INFORMATION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, AWAITING_INFORMATION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, REVIEW_BY_JUDGE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DRAFT_TO_NON_COMPLIANT, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.REINSTATE_APPEAL, AWAITING_ADMIN_ACTION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, NONE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, NONE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.SEND_TO_ADMIN, AWAITING_ADMIN_ACTION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DWP_CHALLENGE_VALIDITY, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DWP_REQUEST_TIME_EXTENSION, REVIEW_BY_TCW.getCcdDefinition());
        return eventTypeToSecondaryStatus;
    }

    protected SscsCaseData setField(SscsCaseData newSscsCaseData, String newValue, EventType eventType) {
        if (newSscsCaseData.isLanguagePreferenceWelsh()) {
            log.info("Case({}): Setting Welsh next review field to {}", newSscsCaseData.getCcdCaseId(), newValue);
            newSscsCaseData.setWelshInterlocNextReviewState(newValue);
            newSscsCaseData.setInterlocReviewState(WELSH_TRANSLATION);
            log.info("Case({}): Set interloc review  field to {}", newSscsCaseData.getCcdCaseId(), newSscsCaseData.getInterlocReviewState());
        } else {
            log.info("Case({}): Setting interloc review field to {}", newSscsCaseData.getCcdCaseId(), newValue);
            InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                .filter(x -> x.getCcdDefinition().equals(newValue))
                .findFirst()
                .orElse(null);
            newSscsCaseData.setInterlocReviewState(interlocState);
        }

        setInterlocReferralDate(newSscsCaseData, eventType);
        clearDirectionDueDate(newSscsCaseData, eventType);

        return newSscsCaseData;
    }

    private void setInterlocReferralDate(SscsCaseData newSscsCaseData, EventType eventType) {
        if (eventType.equals(EventType.NON_COMPLIANT)
                || eventType.equals(EventType.DRAFT_TO_NON_COMPLIANT)
                || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)
                || eventType.equals(EventType.TCW_REFER_TO_JUDGE)
                || eventType.equals(EventType.DWP_REQUEST_TIME_EXTENSION)
                || eventType.equals(EventType.DWP_CHALLENGE_VALIDITY)
                || eventType.equals(EventType.REINSTATE_APPEAL)) {
            log.info("Setting interloc referral date to {} for caseId {}", LocalDate.now().toString(), newSscsCaseData.getCcdCaseId());
            newSscsCaseData.setInterlocReferralDate(LocalDate.now());
        }
    }

    private void clearDirectionDueDate(SscsCaseData newSscsCaseData, EventType eventType) {
        if (eventType.equals(EventType.NON_COMPLIANT)
                || eventType.equals(EventType.DRAFT_TO_NON_COMPLIANT)
                || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)
                || eventType.equals(EventType.REINSTATE_APPEAL)) {
            log.info("Clearing direction due date for caseId {}", newSscsCaseData.getCcdCaseId());
            newSscsCaseData.setDirectionDueDate(null);
        }
    }
}
