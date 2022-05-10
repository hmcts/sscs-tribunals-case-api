package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.GenTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class ScenarioNonDescriptorContent extends GenTemplateContent {

    public ScenarioNonDescriptorContent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(WriteFinalDecisionComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(
            WriteFinalDecisionComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision(), writeFinalDecisionTemplateBody.isHmrc())));
        addComponent(new Paragraph(WriteFinalDecisionComponentId.SUMMARY_OF_OUTCOME_DECISION.name(), writeFinalDecisionTemplateBody.getSummaryOfOutcomeDecision()));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public GenScenario getScenario() {
        return GenScenario.SCENARIO_NON_DESCRIPTOR;
    }
}
