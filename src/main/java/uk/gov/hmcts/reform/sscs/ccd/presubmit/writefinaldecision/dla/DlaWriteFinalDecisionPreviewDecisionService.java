package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
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

            // Validate here for PIP instead of only validating on submit.
            // This ensures that we know we can obtain a valid allowed or refused condition below
            //outcomeService.validate(response, caseData)
            // If validation has produced no errors, we know that we can get an allowed/refused condition.

            // Optional<EsaAllowedOrRefusedCondition> condition = EsaPointsRegulationsAndSchedule3ActivitiesCondition
            //   .getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
            //if (condition.isPresent()) {
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

        String dailyLivingAwardType = caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion();
        String mobilityAwardType = caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion();

        if (dailyLivingAwardType != null) {
            builder.dailyLivingAwardRate(join(
                splitByCharacterTypeCamelCase(dailyLivingAwardType), ' ').toLowerCase());
        } else {
            builder.dailyLivingAwardRate(null);
        }

        if (AwardType.ENHANCED_RATE.getKey().equals(dailyLivingAwardType)) {
            builder.dailyLivingIsEntited(true);
            builder.dailyLivingIsSeverelyLimited(true);
        } else if (AwardType.STANDARD_RATE.getKey().equals(dailyLivingAwardType)) {
            builder.dailyLivingIsEntited(true);
            builder.dailyLivingIsSeverelyLimited(false);
        } else {
            builder.dailyLivingIsEntited(false);
            builder.dailyLivingIsSeverelyLimited(false);
        }

        if (mobilityAwardType != null) {
            builder.mobilityAwardRate(join(
                splitByCharacterTypeCamelCase(mobilityAwardType), ' ').toLowerCase());
        } else {
            builder.mobilityAwardRate(null);
        }

        if (AwardType.ENHANCED_RATE.getKey().equals(mobilityAwardType)) {
            builder.mobilityIsEntited(true);
            builder.mobilityIsSeverelyLimited(true);
        } else if (AwardType.STANDARD_RATE.getKey().equals(mobilityAwardType)) {
            builder.mobilityIsEntited(true);
            builder.mobilityIsSeverelyLimited(false);
        } else {
            builder.mobilityIsEntited(false);
            builder.mobilityIsSeverelyLimited(false);
        }
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        builder.isDescriptorFlow(false);
    }

    private List<String> getConsideredComparisonsWithDwp(SscsCaseData caseData) {
        List<String> consideredComparissons = new ArrayList<>();
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())) {
            consideredComparissons.add(caseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        }
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
            consideredComparissons.add(caseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        }
        return consideredComparissons;
    }
}
