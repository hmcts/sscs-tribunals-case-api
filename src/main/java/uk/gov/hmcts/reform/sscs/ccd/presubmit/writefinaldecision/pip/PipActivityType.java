package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate an ActivityType, along with bindings to the methods on SscsCaseData that yield the specified award type for that ActivityType, and the answers to the multi-select list of
 * activities specified for the ActivityType.
 */
public enum PipActivityType implements ActivityType {

    DAILY_LIVING("Daily Living", SscsPipCaseData::getPipWriteFinalDecisionDailyLivingQuestion,
        SscsPipCaseData::getPipWriteFinalDecisionDailyLivingActivitiesQuestion),
    MOBILITY("Mobility", SscsPipCaseData::getPipWriteFinalDecisionMobilityQuestion,
        SscsPipCaseData::getPipWriteFinalDecisionMobilityActivitiesQuestion);

    final String name;
    final Function<SscsCaseData, String> awardTypeExtractor;
    final Function<SscsCaseData, List<String>> answersExtractor;

    PipActivityType(String name, Function<SscsPipCaseData, String> awardTypeExtractor, Function<SscsPipCaseData, List<String>> answersExtractor) {
        this.name = name;
        this.awardTypeExtractor = c -> awardTypeExtractor.apply(c.getSscsPipCaseData());
        this.answersExtractor = c -> answersExtractor.apply(c.getSscsPipCaseData());
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
