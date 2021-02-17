package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum UcScenario {
    SCENARIO_1(Scenario1Content::new), SCENARIO_2(Scenario2Content::new), SCENARIO_3(Scenario3Content::new),
    SCENARIO_4(Scenario4Content::new), SCENARIO_5(Scenario5Content::new), SCENARIO_6(Scenario6Content::new),
    SCENARIO_7(Scenario7Content::new), SCENARIO_8(Scenario8Content::new), SCENARIO_9(Scenario9Content::new),
    SCENARIO_10(Scenario10Content::new), SCENARIO_12(Scenario12Content::new);

    Function<WriteFinalDecisionTemplateBody, UcTemplateContent> contentSupplier;

    UcScenario(Function<WriteFinalDecisionTemplateBody, UcTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public UcTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
