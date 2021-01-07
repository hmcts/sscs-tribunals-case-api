package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class DailyLivingComparedToDwpCondition extends ComparedToDwpCondition {

    public DailyLivingComparedToDwpCondition(ComparedToDwpPredicate predicate) {
        super(predicate, SscsCaseData::getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion, PipActivityType.DAILY_LIVING);
    }
}
