package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.newscenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcNewTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcTemplateComponentId;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


public class NewScenario9Content extends UcNewTemplateContent {

    public NewScenario9Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(UcTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(
            UcTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(
            UcTemplateComponentId.DOES_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(UcTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH_SCHEDULE_8_PARAGRAPH_4_APPLIED.name(), getInsufficientPointsSentenceSchedule8Paragraph4Applied()));
        addComponent(new DescriptorTable(UcTemplateComponentId.SCHEDULE_6_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getUcSchedule6Descriptors(), false));
        addComponent(new Paragraph(UcTemplateComponentId.DISEASE_OR_DISABLEMENT_PARAGRAPH.name(), getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(true, false)));
        addComponent(new Paragraph(UcTemplateComponentId.SCHEDULE_7_PARAGRAPH.name(), getSchedule7AppliesParagraph(writeFinalDecisionTemplateBody.getUcSchedule7Descriptors())));
        addComponent(new DescriptorTable(UcTemplateComponentId.SCHEDULE_7_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getUcSchedule7Descriptors(), true));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
        addRecommendationIfPresent(writeFinalDecisionTemplateBody);
    }

    @Override
    public UcScenario getScenario() {
        return UcScenario.SCENARIO_9;
    }
}
