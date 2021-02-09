package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.scenarios;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.DlaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum DlaScenario {

    SCENARIO_NON_DESCRIPTOR(ScenarioNonDescriptorContent::new);

    Function<WriteFinalDecisionTemplateBody, DlaTemplateContent> contentSupplier;

    DlaScenario(Function<WriteFinalDecisionTemplateBody, DlaTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public DlaTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
