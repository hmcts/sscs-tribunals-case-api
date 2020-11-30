package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.newscenarios.*;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum UcScenario {
    SCENARIO_1(NewScenario1Content::new), SCENARIO_2(NewScenario2Content::new), SCENARIO_3(NewScenario3Content::new),
    SCENARIO_4(Scenario4Content::new), SCENARIO_5(NewScenario5Content::new), SCENARIO_6(NewScenario6Content::new),
    SCENARIO_7(NewScenario7Content::new), SCENARIO_8(NewScenario8Content::new), SCENARIO_9(NewScenario9Content::new),
    SCENARIO_10(Scenario10Content::new), SCENARIO_12(NewScenario12Content::new);

    Function<WriteFinalDecisionTemplateBody, UcTemplateContent> contentSupplier;

    UcScenario(Function<WriteFinalDecisionTemplateBody, UcTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public UcTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
