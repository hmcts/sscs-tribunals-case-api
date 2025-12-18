package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static java.util.Optional.empty;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.*;

@Slf4j
@Component
public class UcWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    private UcDecisionNoticeQuestionService ucDecisionNoticeQuestionService;
    private VenueDataLoader venueDataLoader;

    @Autowired
    public UcWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
        UcDecisionNoticeQuestionService decisionNoticeQuestionService, UcDecisionNoticeOutcomeService outcomeService, DocumentConfiguration documentConfiguration, VenueDataLoader venueDataLoader) {
        super(generateFile, userDetailsService, decisionNoticeQuestionService, outcomeService, documentConfiguration, venueDataLoader);
        this.ucDecisionNoticeQuestionService = decisionNoticeQuestionService;
        this.venueDataLoader = venueDataLoader;
    }

    @Override
    public String getBenefitType() {
        return "UC";
    }

    @Override
    protected void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData,
        WriteFinalDecisionTemplateBody payload) {

        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            // Validate here for UC instead of only validating on submit.
            // This ensures that we know we can obtain a valid allowed or refused condition below
            outcomeService.validate(response, caseData);
            if (response.getErrors().isEmpty()) {

                // If validation has produced no errors, we know that we can get an allowed/refused condition.
                Optional<UcAllowedOrRefusedCondition> condition = UcPointsRegulationsAndSchedule7ActivitiesCondition
                    .getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
                if (condition.isPresent()) {
                    UcScenario scenario = condition.get().getUcScenario(caseData);
                    UcTemplateContent templateContent = scenario.getContent(payload);
                    builder.writeFinalDecisionTemplateContent(templateContent);
                } else {
                    // Should never happen.
                    log.error("Unable to obtain a valid scenario before preview - Something has gone wrong for caseId: ", caseData.getCcdCaseId());
                    response.addError("Unable to obtain a valid scenario - something has gone wrong");
                }
            }
        }
    }

    @Override
    protected void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            builder.ucIsEntited(false);
            builder.ucAwardRate(null);
            Optional<AwardType> ucAwardTypeOptional = caseData.isWcaAppeal() ? UcPointsRegulationsAndSchedule7ActivitiesCondition
                .getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(decisionNoticeQuestionService, caseData).getAwardType() : empty();
            if (!ucAwardTypeOptional.isEmpty()) {
                String ucAwardType = ucAwardTypeOptional.get().getKey();
                if (ucAwardType != null) {
                    builder.ucAwardRate(join(
                        splitByCharacterTypeCamelCase(ucAwardType), ' ').toLowerCase());
                }

                if (AwardType.LOWER_RATE.getKey().equals(ucAwardType)
                    || AwardType.HIGHER_RATE.getKey().equals(ucAwardType)) {
                    builder.ucIsEntited(true);
                }
            }
        }
        builder.dwpReassessTheAward(caseData.getDwpReassessTheAward());
    }

    protected List<Descriptor> getUcSchedule6DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(key -> ucDecisionNoticeQuestionService.extractQuestionFromKey(UcActivityQuestionKey.getByKey(key)), caseData, questionKeys);
    }

    protected List<Descriptor> getUcSchedule7DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(key -> ucDecisionNoticeQuestionService.extractQuestionFromKey(UcSchedule7QuestionKey.getByKey(key)), caseData, questionKeys);
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        builder.wcaAppeal(caseData.isWcaAppeal());

        List<Descriptor> allSchedule6Descriptors = new ArrayList<>();
        List<String> physicalDisabilityAnswers = UcActivityType.PHYSICAL_DISABILITIES.getAnswersExtractor().apply(caseData);
        if (physicalDisabilityAnswers != null) {
            List<Descriptor> physicalDisablityDescriptors = getUcSchedule6DescriptorsFromQuestionKeys(caseData, physicalDisabilityAnswers);
            allSchedule6Descriptors.addAll(physicalDisablityDescriptors);
        }
        List<String> mentalAssessmentAnswers = UcActivityType.MENTAL_ASSESSMENT.getAnswersExtractor().apply(caseData);
        if (mentalAssessmentAnswers != null) {
            List<Descriptor> mentalAssessmentDescriptors = getUcSchedule6DescriptorsFromQuestionKeys(caseData, mentalAssessmentAnswers);
            allSchedule6Descriptors.addAll(mentalAssessmentDescriptors);
        }

        // Don't add descriptors to the template if the total points in schedule 6 are zero
        int totalPoints = allSchedule6Descriptors.stream().mapToInt(d -> d.getActivityAnswerPoints()).sum();
        if (totalPoints == 0) {
            allSchedule6Descriptors.clear();
        }

        if (allSchedule6Descriptors.isEmpty()) {
            if (caseData.isWcaAppeal()) {
                if (caseData.isSupportGroupOnlyAppeal()) {
                    builder.ucSchedule6Descriptors(null);
                    builder.ucNumberOfPoints(null);
                } else {
                    builder.ucSchedule6Descriptors(new ArrayList<>());
                    builder.ucNumberOfPoints(0);
                }
            } else {
                builder.ucSchedule6Descriptors(null);
                builder.ucNumberOfPoints(null);
            }
        } else {
            builder.ucSchedule6Descriptors(allSchedule6Descriptors);
            int numberOfPoints = allSchedule6Descriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum();
            if (UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(numberOfPoints)) {
                caseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(null);
            }
            builder.ucNumberOfPoints(numberOfPoints);
        }
        if (caseData.getSscsUcCaseData().getSchedule7Selections() != null && !caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty()) {
            builder.ucSchedule7Descriptors(getUcSchedule7DescriptorsFromQuestionKeys(caseData, caseData.getSscsUcCaseData().getSchedule7Selections()));
        }
        builder.schedule8Paragraph4Applicable(caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply() == null ? null :  caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply().toBoolean());
        builder.schedule9Paragraph4Applicable(caseData.getSscsUcCaseData().getDoesSchedule9Paragraph4Apply() == null ? null :  caseData.getSscsUcCaseData().getDoesSchedule9Paragraph4Apply().toBoolean());
        builder.supportGroupOnly(caseData.isSupportGroupOnlyAppeal());
    }


}
