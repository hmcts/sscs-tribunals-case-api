package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.apache.commons.lang3.StringUtils.*;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


public class Scenario7Content extends EsaTemplateContent {

    public Scenario7Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(EsaTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(EsaTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName(), true, false, false)));
        addComponent(new Paragraph(EsaTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH_REGULATION_29_APPLIED.name(), getInsufficientPointsSentenceRegulation29Applied()));
        addComponent(new Paragraph(EsaTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule2PointsSentence(writeFinalDecisionTemplateBody.getEsaNumberOfPoints(), true)));
        addComponent(new DescriptorTable(EsaTemplateComponentId.SCHEDULE_2_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getEsaSchedule2Descriptors(), false));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_RELATED_ACTIVITY_PARAGRAPH.name(), getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(EsaTemplateComponentId.DISEASE_OR_DISABLEMENT_PARAGRAPH.name(), getRegulation29And35DiseaseOrDisablementSentence(writeFinalDecisionTemplateBody.getAppellantName(), false)));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addComponent(new Paragraph(EsaTemplateComponentId.HEARING_TYPE.name(), getHearingTypeSentence(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber())));
        if (!writeFinalDecisionTemplateBody.isWcaAppeal() && isNotBlank(writeFinalDecisionTemplateBody.getDwpReassessTheAward())) {
            addComponent(new Paragraph(EsaTemplateComponentId.RECOMMENDATION.name(), getRecommendationSentence(writeFinalDecisionTemplateBody.getDwpReassessTheAward(), writeFinalDecisionTemplateBody.getAppellantName())));
        }

    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_7;
    }
}
