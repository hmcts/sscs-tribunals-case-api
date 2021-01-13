package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

@Slf4j
@Component
public class PipWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    @Autowired
    public PipWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        PipDecisionNoticeQuestionService decisionNoticeQuestionService, PipDecisionNoticeOutcomeService decisionNoticeOutcomeService, DocumentConfiguration documentConfiguration) {
        super(generateFile, idamClient, decisionNoticeQuestionService, decisionNoticeOutcomeService, documentConfiguration);
    }

    @Override
    public String getBenefitType() {
        return "PIP";
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
            PipScenario scenario = PipScenario.SCENARIO_1;
            PipTemplateContent templateContent = scenario.getContent(payload);
            builder.writeFinalDecisionTemplateContent(templateContent);
            // } else {
            // Should never happen.
            //  log.error("Unable to obtain a valid scenario before preview - Something has gone wrong for caseId: ", caseData.getCcdCaseId());
            //  response.addError("Unable to obtain a valid scenario - something has gone wrong");
            //}
        }
    }

    @Override
    protected void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        String dailyLivingAwardType = caseData.getPipWriteFinalDecisionDailyLivingQuestion();
        String mobilityAwardType = caseData.getPipWriteFinalDecisionMobilityQuestion();

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
        builder.isDescriptorFlow(caseData.isDailyLivingAndOrMobilityDecision());

        List<String> dailyLivingAnswers = PipActivityType.DAILY_LIVING.getAnswersExtractor().apply(caseData);
        if (dailyLivingAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {

            List<Descriptor> dailyLivingDescriptors = getPipDescriptorsFromQuestionKeys(caseData, dailyLivingAnswers);

            builder.dailyLivingNumberOfPoints(dailyLivingDescriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum());

            builder.dailyLivingDescriptors(dailyLivingDescriptors);
        } else {
            builder.dailyLivingDescriptors(null);
            builder.dailyLivingNumberOfPoints(null);
        }

        List<String> mobilityAnswers = PipActivityType.MOBILITY.getAnswersExtractor().apply(caseData);
        if (mobilityAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
            List<Descriptor> mobilityDescriptors = getPipDescriptorsFromQuestionKeys(caseData, mobilityAnswers);

            builder.mobilityDescriptors(mobilityDescriptors);

            builder.mobilityNumberOfPoints(mobilityDescriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum());
        } else {
            builder.mobilityDescriptors(null);
            builder.mobilityNumberOfPoints(null);
        }
    }

    protected List<Descriptor> getPipDescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(PipActivityQuestion::getByKey, caseData, questionKeys);
    }

    @Override
    protected boolean isSetAside(SscsCaseData sscsCaseData, Outcome outcome) {
        if ("yes".equalsIgnoreCase((sscsCaseData.getWriteFinalDecisionIsDescriptorFlow()))
            && "yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {
            return getConsideredComparisonsWithDwp(sscsCaseData).stream().anyMatch(comparission -> !"same".equalsIgnoreCase(comparission));
        } else {
            return super.isSetAside(sscsCaseData, outcome);
        }
    }

    private List<String> getConsideredComparisonsWithDwp(SscsCaseData caseData) {
        List<String> consideredComparissons = new ArrayList<>();
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {
            consideredComparissons.add(caseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        }
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
            consideredComparissons.add(caseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        }
        return consideredComparissons;
    }
}
