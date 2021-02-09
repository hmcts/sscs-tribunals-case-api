package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.scenarios.DlaScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DlaDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DlaDecisionNoticeQuestionService;

@Slf4j
@Component
public class DlaWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    @Autowired
    public DlaWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        DlaDecisionNoticeQuestionService decisionNoticeQuestionService, DlaDecisionNoticeOutcomeService decisionNoticeOutcomeService, DocumentConfiguration documentConfiguration) {
        super(generateFile, idamClient, decisionNoticeQuestionService, decisionNoticeOutcomeService, documentConfiguration);
    }

    @Override
    public String getBenefitType() {
        return "DLA";
    }

    @Override
    protected Descriptor buildDescriptorFromActivityAnswer(ActivityQuestion activityQuestion, ActivityAnswer answer) {
        return Descriptor.builder().activityAnswerPoints(answer.getActivityAnswerPoints())
            .activityQuestionNumber(answer.getActivityAnswerNumber())
            .activityAnswerLetter(answer.getActivityAnswerLetter())
            .activityAnswerValue(answer.getActivityAnswerValue())
            .activityQuestionValue(answer.getActivityAnswerNumber() + ". " + activityQuestion.getValue())
            .build();
    }

    @Override
    protected void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData,
        WriteFinalDecisionTemplateBody payload) {


        if ("Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {

            Optional<DlaAllowedOrRefusedCondition> condition = DlaAllowedOrRefusedCondition.getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
            if (condition.isPresent()) {
                DlaAllowedOrRefusedCondition allowedOrRefusedCondition = condition.get();
                DlaScenario scenario = allowedOrRefusedCondition.getPipScenario(caseData);
                if (scenario != null) {
                    DlaTemplateContent templateContent = scenario.getContent(payload);
                    builder.writeFinalDecisionTemplateContent(templateContent);
                }
            } else {
                // Should never happen.
                log.error("Unable to obtain a valid scenario before preview - Something has gone wrong for caseId: ", caseData.getCcdCaseId());
                response.addError("Unable to obtain a valid scenario - something has gone wrong");
            }
        }
    }

    @Override
    protected void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        // No-op for DLA
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        builder.isDescriptorFlow(false);
    }
}
