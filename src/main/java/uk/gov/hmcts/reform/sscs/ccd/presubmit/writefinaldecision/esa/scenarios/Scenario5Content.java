package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario5Content extends EsaTemplateContent {

    public Scenario5Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(WriteFinalDecisionComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(WriteFinalDecisionComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName(), false, false, false)));
        addComponent(new Paragraph(EsaTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule2PointsSentence(writeFinalDecisionTemplateBody.getEsaNumberOfPoints(), true,
            writeFinalDecisionTemplateBody.getEsaSchedule2Descriptors())));
        addDescriptorTableIfPopulated(new DescriptorTable(EsaTemplateComponentId.SCHEDULE_2_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getEsaSchedule2Descriptors(), false));
        addComponent(new Paragraph(EsaTemplateComponentId.SCHEDULE_3_PARAGRAPH.name(), getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
        addRecommendationIfPresent(writeFinalDecisionTemplateBody);
    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_5;
    }
}
