package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;

public class MobilityComparedToDwpCondition extends ComparedToDwpCondition {

    public MobilityComparedToDwpCondition(ComparedToDwpPredicate predicate) {
        super(predicate, SscsPipCaseData::getPipWriteFinalDecisionComparedToDwpMobilityQuestion, PipActivityType.MOBILITY);
    }
}
