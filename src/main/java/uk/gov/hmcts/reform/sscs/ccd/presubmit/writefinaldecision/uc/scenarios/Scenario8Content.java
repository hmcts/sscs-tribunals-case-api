package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario8Content extends UcTemplateContent {

    public Scenario8Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(UcTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(
            UcTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(
            UcTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName(), true, true, false)));
        addComponent(new Paragraph(UcTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH_SCHEDULE_8_PARAGRAPH_4_APPLIED.name(), getInsufficientPointsSentenceSchedule8Paragraph4AndSchedule9Paragraph4Applied()));
        addComponent(new Paragraph(UcTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule2PointsSentence(writeFinalDecisionTemplateBody.getUcNumberOfPoints(), true)));
        addComponent(new DescriptorTable(UcTemplateComponentId.SCHEDULE_6_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getUcSchedule6Descriptors(), false));
        addComponent(new Paragraph(
            UcTemplateComponentId.DISEASE_OR_DISABLEMENT_PARAGRAPH.name(), getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(writeFinalDecisionTemplateBody.getAppellantName(), true)));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addComponent(new Paragraph(UcTemplateComponentId.HEARING_TYPE.name(), getHearingTypeSentence(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber())));
        addRecommendationIfPresent(writeFinalDecisionTemplateBody);
    }

    @Override
    public UcScenario getScenario() {
        return UcScenario.SCENARIO_8;
    }
}
