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
        if ("not considered".equals(writeFinalDecisionTemplateBody.getDailyLivingAwardRate())) {
            addComponent(new Paragraph(PipTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getDailyLivingNotConsidered()));
        } else {
            if (writeFinalDecisionTemplateBody.isDailyLivingIsEntited()) {
                addComponent(new Paragraph(PipTemplateComponentId.IS_ENTITLED_DAILY_LIVING_PARAGRAPH.name(),
                    getIsEntitledDailyLiving(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getDailyLivingAwardRate(),
                        writeFinalDecisionTemplateBody.getStartDate())));
                addComponent(new Paragraph(PipTemplateComponentId.LIMITED_ABILITY_DAILY_LIVING_PARAGRAPH.name(),
                    getLimitedAbilityDailyLiving(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getDailyLivingNumberOfPoints(),
                        writeFinalDecisionTemplateBody.getDailyLivingDescriptors(), writeFinalDecisionTemplateBody.isDailyLivingIsSeverelyLimited())));
            } else {
                if (writeFinalDecisionTemplateBody.getDailyLivingNumberOfPoints() != null) {
                    addComponent(new Paragraph(PipTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(),
                        getDailyLivingNoAward(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getStartDate(),
                            writeFinalDecisionTemplateBody.getDailyLivingNumberOfPoints())));
                }
            }
        }

        if (!"not considered".equals(writeFinalDecisionTemplateBody.getDailyLivingAwardRate())) {
            addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.DAILY_LIVING_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getDailyLivingDescriptors(), false, true));
        }
        if ("not considered".equals(writeFinalDecisionTemplateBody.getMobilityAwardRate())) {
            addComponent(new Paragraph(PipTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getMobilityNotConsidered()));
        } else {
            if (writeFinalDecisionTemplateBody.isMobilityIsEntited()) {
                addComponent(new Paragraph(PipTemplateComponentId.IS_ENTITLED_MOBILITY_PARAGRAPH.name(),
                    getIsEntitledMobility(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getMobilityAwardRate(), writeFinalDecisionTemplateBody.getStartDate())));
                addComponent(new Paragraph(PipTemplateComponentId.LIMITED_ABILITY_MOBILITY_PARAGRAPH.name(),
                    getLimitedAbilityMobility(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getMobilityNumberOfPoints(),
                        writeFinalDecisionTemplateBody.getMobilityDescriptors(), writeFinalDecisionTemplateBody.isMobilityIsSeverelyLimited())));
            } else {
                if (writeFinalDecisionTemplateBody.getMobilityNumberOfPoints() != null) {
                    addComponent(new Paragraph(PipTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(),
                        getMobilityNoAward(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getStartDate(),
                            writeFinalDecisionTemplateBody.getMobilityNumberOfPoints())));
                }
            }
        }

        addDescriptorTableIfPopulated(new DescriptorTable(PipTemplateComponentId.MOBILITY_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getMobilityDescriptors(), false, true));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public PipScenario getScenario() {
        return PipScenario.SCENARIO_1;
    }
}
