package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario1Content extends EsaTemplateContent {

    public Scenario1Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(EsaTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(EsaTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));
        addComponent(new Paragraph(EsaTemplateComponentId.DOES_NOT_HAVE_LIMITED_CAPABILiTY_FOR_WORK_PARAGRAPH.name(), getDoesNotHaveLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(EsaTemplateComponentId.INSUFFICIENT_POINTS_PARAGRAPH.name(), getSchedule2InsuffientPointsSentence(writeFinalDecisionTemplateBody.getEsaNumberOfPoints(), writeFinalDecisionTemplateBody.getRegulation29Applicable())));
        addComponent(new DescriptorTable(EsaTemplateComponentId.SCHEDULE_2_DESCRIPTORS.name(), writeFinalDecisionTemplateBody.getEsaSchedule2Descriptors(), false));
        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(EsaTemplateComponentId.REASON.name(), reason));
            }
        }
        if (writeFinalDecisionTemplateBody.getAnythingElse() != null) {
            addComponent(new Paragraph(EsaTemplateComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
    }

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_1;
    }
}
