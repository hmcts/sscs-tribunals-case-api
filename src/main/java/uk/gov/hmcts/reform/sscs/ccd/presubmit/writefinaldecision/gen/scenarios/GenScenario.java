package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.GenTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum GenScenario {

    SCENARIO_NON_DESCRIPTOR(ScenarioNonDescriptorContent::new);

    Function<WriteFinalDecisionTemplateBody, GenTemplateContent> contentSupplier;

    GenScenario(Function<WriteFinalDecisionTemplateBody, GenTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public GenTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
