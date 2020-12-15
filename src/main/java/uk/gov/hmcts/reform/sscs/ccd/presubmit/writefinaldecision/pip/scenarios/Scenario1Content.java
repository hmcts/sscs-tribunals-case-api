package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario1Content extends PipTemplateContent {

    public Scenario1Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(PipTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(PipTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(PipTemplateComponentId.IS_ENTITLED_DAILY_LIVING_PARAGRAPH.name(), getIsEntitledDailyLiving(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(PipTemplateComponentId.LIMITED_ABILITY_DAILY_LIVING_PARAGRAPH.name(), getLimitedAbilityDailyLiving(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getDailyLivingNumberOfPoints(), writeFinalDecisionTemplateBody.getDailyLivingDescriptors())));
        addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.DAILY_LIVING_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getDailyLivingDescriptors(), false));
        addComponent(new Paragraph(PipTemplateComponentId.IS_ENTITLED_MOBILITY_PARAGRAPH.name(), getIsEntitledMobility(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(PipTemplateComponentId.LIMITED_ABILITY_MOBILITY_PARAGRAPH.name(), getLimitedAbilityMobility(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getMobilityNumberOfPoints(), writeFinalDecisionTemplateBody.getMobilityDescriptors())));
        addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.MOBILITY_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getMobilityDescriptors(), false));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public PipScenario getScenario() {
        return PipScenario.SCENARIO_1;
    }
}
