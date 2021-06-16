package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate an ActivityType, along with bindings to the methods on SscsCaseData that yield the specified award type for that ActivityType, and the answers to the multi-select list of
 * activities specified for the ActivityType.
 */
public enum EsaActivityType implements ActivityType {

    PHYSICAL_DISABILITIES("Physical Disabilities",
        SscsEsaCaseData::getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion),
    MENTAL_ASSESSMENT("Mental, cognitive and intellectual function assessment",
        SscsEsaCaseData::getEsaWriteFinalDecisionMentalAssessmentQuestion);

    final String name;
    final Function<SscsCaseData, List<String>> answersExtractor;

    EsaActivityType(String name, Function<SscsEsaCaseData, List<String>> answersExtractor) {
        this.name = name;
        this.answersExtractor = c -> answersExtractor.apply(c.getSscsEsaCaseData());
    }

    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return answersExtractor;
    }

    public String getName() {
        return name;
    }
}
