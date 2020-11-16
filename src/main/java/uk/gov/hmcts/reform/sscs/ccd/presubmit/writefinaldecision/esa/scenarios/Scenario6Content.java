package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario6Content extends EsaTemplateContent {

    public Scenario6Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(EsaTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(EsaTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName(), false, true, false)));
        addComponent(new Paragraph(EsaTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule2PointsSentence(writeFinalDecisionTemplateBody.getEsaNumberOfPoints(), true)));
        addComponent(new DescriptorTable(EsaTemplateComponentId.SCHEDULE_2_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getEsaSchedule2Descriptors(), false));
        addComponent(new Paragraph(EsaTemplateComponentId.SCHEDULE_3_PARAGRAPH.name(), getSchedule3AppliesParagraph()));
        addComponent(new DescriptorTable(EsaTemplateComponentId.SCHEDULE_3_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getEsaSchedule3Descriptors(), true));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addComponent(new Paragraph(EsaTemplateComponentId.HEARING_TYPE.name(), getHearingTypeSentence(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber())));
        addRecommendationIfPresent(writeFinalDecisionTemplateBody);
    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_6;
    }
}
