package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.*;

@Slf4j
@Component
public class PipWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    @Autowired
    public PipWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
                                                       PipDecisionNoticeQuestionService decisionNoticeQuestionService, PipDecisionNoticeOutcomeService decisionNoticeOutcomeService, DocumentConfiguration documentConfiguration, VenueDataLoader venueDataLoader) {
        super(generateFile, userDetailsService, decisionNoticeQuestionService, decisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
    }

    @Override
    public String getBenefitType() {
        return "PIP";
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

        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            Optional<PipAllowedOrRefusedCondition> condition = PipAllowedOrRefusedCondition.getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
            if (condition.isPresent()) {
                PipAllowedOrRefusedCondition allowedOrRefusedCondition = condition.get();
                PipScenario scenario = allowedOrRefusedCondition.getPipScenario(caseData);
                if (scenario != null) {
                    PipTemplateContent templateContent = scenario.getContent(payload);
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
        builder.isDescriptorFlow(caseData.getSscsFinalDecisionCaseData().isDailyLivingAndOrMobilityDecision());

        List<String> dailyLivingAnswers = PipActivityType.DAILY_LIVING.getAnswersExtractor().apply(caseData);
        if (dailyLivingAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())) {

            List<Descriptor> dailyLivingDescriptors = getPipDescriptorsFromQuestionKeys(caseData, dailyLivingAnswers);

            builder.dailyLivingNumberOfPoints(dailyLivingDescriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum());

            builder.dailyLivingDescriptors(dailyLivingDescriptors);
        } else {
            builder.dailyLivingDescriptors(null);
            builder.dailyLivingNumberOfPoints(null);
        }

        List<String> mobilityAnswers = PipActivityType.MOBILITY.getAnswersExtractor().apply(caseData);
        if (mobilityAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
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
        if (isYes((sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()))
            && isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            return getConsideredComparisonsWithDwp(sscsCaseData).stream().anyMatch(comparission -> !"same".equalsIgnoreCase(comparission));
        } else {
            return super.isSetAside(sscsCaseData, outcome);
        }
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
