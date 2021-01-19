package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class MobilityComparedToDwpCondition extends ComparedToDwpCondition {

    public MobilityComparedToDwpCondition(ComparedToDwpPredicate predicate) {
        super(predicate, SscsCaseData::getPipWriteFinalDecisionComparedToDwpMobilityQuestion, PipActivityType.MOBILITY);
    }
}
