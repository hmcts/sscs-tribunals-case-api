package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate an ActivityType, along with bindings to the methods on SscsCaseData that yield the specified award type for that ActivityType, and the answers to the multi-select list of
 * activities specified for the ActivityType.
 */
public enum PipActivityType implements ActivityType {

    DAILY_LIVING("Daily Living", SscsCaseData::getPipWriteFinalDecisionDailyLivingQuestion,
        SscsCaseData::getPipWriteFinalDecisionDailyLivingActivitiesQuestion),
    MOBILITY("Mobility", SscsCaseData::getPipWriteFinalDecisionMobilityQuestion,
        SscsCaseData::getPipWriteFinalDecisionMobilityActivitiesQuestion);

    final String name;
    final Function<SscsCaseData, String> awardTypeExtractor;
    final Function<SscsCaseData, List<String>> answersExtractor;

    PipActivityType(String name, Function<SscsCaseData, String> awardTypeExtractor, Function<SscsCaseData, List<String>> answersExtractor) {
        this.name = name;
        this.awardTypeExtractor = awardTypeExtractor;
        this.answersExtractor = answersExtractor;
    }

    public Function<SscsCaseData, String> getAwardTypeExtractor() {
        return awardTypeExtractor;
    }

    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return answersExtractor;
    }

    public String getName() {
        return name;
    }
}
