package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.function.Function;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum PipScenario {

    SCENARIO_1(Scenario1Content::new);

    Function<WriteFinalDecisionTemplateBody, PipTemplateContent> contentSupplier;

    PipScenario(Function<WriteFinalDecisionTemplateBody, PipTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public PipTemplateContent getContent(WriteFinalDecisionTemplateBody body) {
        return contentSupplier.apply(body);
    }
}
