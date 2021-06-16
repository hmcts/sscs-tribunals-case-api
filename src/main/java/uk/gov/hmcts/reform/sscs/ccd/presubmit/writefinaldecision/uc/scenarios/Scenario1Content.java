package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario1Content extends UcTemplateContent {

    public Scenario1Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(WriteFinalDecisionComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(
            WriteFinalDecisionComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(UcTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_RELATED_ACTIVITY_PARAGRAPH.name(), getDoesNotHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(UcTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule6PointsSentence(writeFinalDecisionTemplateBody.getUcNumberOfPoints(), false, writeFinalDecisionTemplateBody.getUcSchedule6Descriptors())));
        addDescriptorTableIfPopulated(new DescriptorTable(UcTemplateComponentId.SCHEDULE_6_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getUcSchedule6Descriptors(), false));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public UcScenario getScenario() {
        return UcScenario.SCENARIO_1;
    }
}
