package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class ScenarioAwardNoAwardContent extends PipTemplateContent {

    public ScenarioAwardNoAwardContent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(WriteFinalDecisionComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(WriteFinalDecisionComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));

        addComponent(new Paragraph(PipTemplateComponentId.IS_ENTITLED_DAILY_LIVING_PARAGRAPH.name(),
                getIsEntitledDailyLiving(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getDailyLivingAwardRate(),
                        writeFinalDecisionTemplateBody.getStartDate(), writeFinalDecisionTemplateBody.getEndDate())));
        addComponent(new Paragraph(PipTemplateComponentId.LIMITED_ABILITY_DAILY_LIVING_PARAGRAPH.name(),
                getLimitedAbilityDailyLiving(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getDailyLivingNumberOfPoints(),
                        writeFinalDecisionTemplateBody.getDailyLivingDescriptors(), writeFinalDecisionTemplateBody.isDailyLivingIsSeverelyLimited())));

        addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.DAILY_LIVING_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getDailyLivingDescriptors(), false));

       
        addComponent(new Paragraph(PipTemplateComponentId.MOBILITY_NO_AWARD_PARAGRAPH.name(),
                getMobilityNoAward(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getStartDate(),
                        writeFinalDecisionTemplateBody.getMobilityNumberOfPoints())));


        addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.MOBILITY_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getMobilityDescriptors(), false));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public PipScenario getScenario() {
        return PipScenario.SCENARIO_AWARD_NO_AWARD;
    }
}
