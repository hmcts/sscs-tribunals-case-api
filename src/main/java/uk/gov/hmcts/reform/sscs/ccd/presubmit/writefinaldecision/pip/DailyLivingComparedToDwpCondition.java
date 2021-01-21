package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;

public class DailyLivingComparedToDwpCondition extends ComparedToDwpCondition {

    public DailyLivingComparedToDwpCondition(ComparedToDwpPredicate predicate) {
        super(predicate, SscsPipCaseData::getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion, PipActivityType.DAILY_LIVING);
    }
}
