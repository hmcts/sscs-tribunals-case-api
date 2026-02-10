package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.esa.scenarios;

import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.WriteFinalDecisionComponentId;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario2Content extends EsaTemplateContent {

    public Scenario2Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        super(writeFinalDecisionTemplateBody);
        addComponent(new Paragraph(WriteFinalDecisionComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(
            WriteFinalDecisionComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision(), writeFinalDecisionTemplateBody.isHmrc(), writeFinalDecisionTemplateBody.isIbca())));
        addComponent(new Paragraph(EsaTemplateComponentId.LIMITED_WORK_BUT_DOES_NOT_HAVE_LIMITED_CAPABILITY_FOR_WORK_PARAGRAPH.name(), getContinuesToHaveWorkRelatedSentenceButNotLimitedWorkRelatedActivity(writeFinalDecisionTemplateBody.getAppellantName())));
        addComponent(new Paragraph(EsaTemplateComponentId.SCHEDULE_3_PARAGRAPH.name(), getNoDescriptorFromSchedule3Regulation35NotApplied()));
        addComponent(new Paragraph(EsaTemplateComponentId.HAS_LIMITED_CAPABILITY_FOR_WORK.name(), getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(writeFinalDecisionTemplateBody.getAppellantName(), true)));
        addReasonsIfPresent(writeFinalDecisionTemplateBody);
        addAnythingElseIfPresent(writeFinalDecisionTemplateBody);
        addHearingType(writeFinalDecisionTemplateBody);
    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_2;
    }
}
