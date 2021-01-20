package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum PipScenario {

    SCENARIO_NON_DESCRIPTOR(ScenarioNonDescriptorContent::new), SCENARIO_NOT_CONSIDERED_NO_AWARD(ScenarioNotConsideredNoAwardContent::new), SCENARIO_AWARD_NOT_CONSIDERED(ScenarioAwardNotConsideredContent::new), SCENARIO_NOT_CONSIDERED_AWARD(ScenarioNotConsideredAwardContent::new), SCENARIO_NO_AWARD_AWARD(ScenarioNoAwardAwardContent::new), SCENARIO_AWARD_AWARD(ScenarioAwardAwardContent::new), SCENARIO_AWARD_NO_AWARD(ScenarioAwardNoAwardContent::new),  SCENARIO_NO_AWARD_NOT_CONSIDERED(ScenarioNoAwardNotConsideredContent::new), SCENARIO_NO_AWARD_NO_AWARD(ScenarioNoAwardNoAwardContent::new);

    Function<WriteFinalDecisionTemplateBody, PipTemplateContent> contentSupplier;

    PipScenario(Function<WriteFinalDecisionTemplateBody, PipTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public PipTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
