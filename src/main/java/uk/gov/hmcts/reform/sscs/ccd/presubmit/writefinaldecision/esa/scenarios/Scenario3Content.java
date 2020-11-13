package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario3Content extends EsaTemplateContent {

    public Scenario3Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(EsaTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(EsaTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getDoesHaveLimitedCapabilityForWorkRelatedSentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_RELATED_ACTIVITY_PARAGRAPH.name(), getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_RELATED_ACTIVITY_PARAGRAPH.name(), getHasLimitedCapabilityForWorkNoSchedule3SentenceReg35Applies()));
        addComponent(new Paragraph(EsaTemplateComponentId.DISEASE_OR_DISABLEMENT_PARAGRAPH.name(), getRegulation35DiseaseOrDisablementSentenceWorkRelated(writeFinalDecisionTemplateBody.getAppellantName())));

        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(EsaTemplateComponentId.REASON.name(), reason));
            }
        }
        if (writeFinalDecisionTemplateBody.getAnythingElse() != null) {
            addComponent(new Paragraph(EsaTemplateComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_3;
    }
}
