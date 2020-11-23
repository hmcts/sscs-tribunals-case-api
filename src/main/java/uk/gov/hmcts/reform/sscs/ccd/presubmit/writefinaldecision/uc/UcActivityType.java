package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate an ActivityType, along with bindings to the methods on SscsCaseData that yield the specified award type for that ActivityType, and the answers to the multi-select list of
 * activities specified for the ActivityType.
 */
public enum UcActivityType implements ActivityType {

    PHYSICAL_DISABILITIES("Physical Disabilities",
        SscsUcCaseData::getUcWriteFinalDecisionPhysicalDisabilitiesQuestion),
    MENTAL_ASSESSMENT("Mental, cognitive and intellectual function assessment",
        SscsUcCaseData::getUcWriteFinalDecisionMentalAssessmentQuestion);

    final String name;
    final Function<SscsCaseData, List<String>> answersExtractor;

    UcActivityType(String name, Function<SscsUcCaseData, List<String>> answersExtractor) {
        this.name = name;
        this.answersExtractor = c -> answersExtractor.apply(c.getSscsUcCaseData());
    }

    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return answersExtractor;
    }

    public String getName() {
        return name;
    }
}
