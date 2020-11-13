package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario10Content extends EsaTemplateContent {

    public Scenario10Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(EsaTemplateComponentId.ALLOWED_OR_REFUSED_PARAGRAPH.name(), getAllowedOrRefusedSentence(writeFinalDecisionTemplateBody.isAllowed())));
        addComponent(new Paragraph(EsaTemplateComponentId.CONFIRMED_OR_SET_ASIDE_PARAGRAPH.name(), getConfirmedOrSetAsideSentence(writeFinalDecisionTemplateBody.isSetAside(), writeFinalDecisionTemplateBody.getDateOfDecision())));

        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(EsaTemplateComponentId.REASON.name(), reason));
            }
        }
        if (writeFinalDecisionTemplateBody.getAnythingElse() != null) {
            addComponent(new Paragraph(EsaTemplateComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
        addComponent(new Paragraph(EsaTemplateComponentId.HEARING_TYPE.name(), getHearingTypeSentence(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber())));
        if (!writeFinalDecisionTemplateBody.isWcaAppeal() && isNotBlank(writeFinalDecisionTemplateBody.getDwpReassessTheAward())) {
            addComponent(new Paragraph(EsaTemplateComponentId.RECOMMENDATION.name(), getRecommendationSentence(writeFinalDecisionTemplateBody.getDwpReassessTheAward(), writeFinalDecisionTemplateBody.getAppellantName())));
        }

    }

    @Override
    public EsaScenario getScenario() {
        return EsaScenario.SCENARIO_10;
    }
}
