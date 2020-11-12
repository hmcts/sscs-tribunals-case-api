package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Optional;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public enum EsaScenario {

    SCENARIO_1(Scenario1Content::new), SCENARIO_2(null), SCENARIO_3(null),
    SCENARIO_4(null), SCENARIO_5(null), SCENARIO_6(null),
    SCENARIO_7(null), SCENARIO_8(null), SCENARIO_9(null);

    Function<WriteFinalDecisionTemplateBody, EsaTemplateContent> contentSupplier;

    EsaScenario(Function<WriteFinalDecisionTemplateBody, EsaTemplateContent> contentSupplier) {
        this.contentSupplier = contentSupplier;
    }

    public Optional<EsaTemplateContent> getContent(WriteFinalDecisionTemplateBody body) {
        if (contentSupplier == null) {
            return Optional.empty();
        } else {
            return Optional.of(contentSupplier.apply(body));
        }
    }
}
