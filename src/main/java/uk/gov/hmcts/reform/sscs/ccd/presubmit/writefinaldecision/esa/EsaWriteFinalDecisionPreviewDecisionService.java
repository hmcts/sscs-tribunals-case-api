package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.*;

@Slf4j
@Component
public class EsaWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    private EsaDecisionNoticeQuestionService esaDecisionNoticeQuestionService;
    private VenueDataLoader venueDataLoader;

    @Autowired
    public EsaWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
        EsaDecisionNoticeQuestionService decisionNoticeQuestionService, EsaDecisionNoticeOutcomeService outcomeService, DocumentConfiguration documentConfiguration, VenueDataLoader venueDataLoader) {
        super(generateFile, userDetailsService, decisionNoticeQuestionService, outcomeService, documentConfiguration, venueDataLoader);
        this.esaDecisionNoticeQuestionService = decisionNoticeQuestionService;
        this.venueDataLoader = venueDataLoader;
    }

    @Override
    public String getBenefitType() {
        return "ESA";
    }

    @Override
    protected void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData,
        WriteFinalDecisionTemplateBody payload) {

        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {

            // Validate here for ESA instead of only validating on submit.
            // This ensures that we know we can obtain a valid allowed or refused condition below
            outcomeService.validate(response, caseData);
            if (response.getErrors().isEmpty()) {

                // If validation has produced no errors, we know that we can get an allowed/refused condition.
                Optional<EsaAllowedOrRefusedCondition> condition = EsaPointsRegulationsAndSchedule3ActivitiesCondition
                    .getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
                if (condition.isPresent()) {
                    EsaScenario scenario = condition.get().getEsaScenario(caseData);
                    EsaTemplateContent templateContent = scenario.getContent(payload);
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
            builder.esaIsEntited(false);
            builder.esaAwardRate(null);
            Optional<AwardType> esaAwardTypeOptional = caseData.isWcaAppeal() ? EsaPointsRegulationsAndSchedule3ActivitiesCondition
                .getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(decisionNoticeQuestionService, caseData).getAwardType() : empty();
            if (!esaAwardTypeOptional.isEmpty()) {
                String esaAwardType = esaAwardTypeOptional.get().getKey();
                if (esaAwardType != null) {
                    builder.esaAwardRate(join(
                        splitByCharacterTypeCamelCase(esaAwardType), ' ').toLowerCase());
                }

                if (AwardType.LOWER_RATE.getKey().equals(esaAwardType)
                    || AwardType.HIGHER_RATE.getKey().equals(esaAwardType)) {
                    builder.esaIsEntited(true);
                }
            }
        }
        builder.dwpReassessTheAward(caseData.getDwpReassessTheAward());
    }

    protected List<Descriptor> getEsaSchedule2DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(key -> esaDecisionNoticeQuestionService.extractQuestionFromKey(EsaActivityQuestionKey.getByKey(key)), caseData, questionKeys);
    }

    protected List<Descriptor> getEsaSchedule3DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(key -> esaDecisionNoticeQuestionService.extractQuestionFromKey(EsaSchedule3QuestionKey.getByKey(key)), caseData, questionKeys);
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        builder.wcaAppeal(caseData.isWcaAppeal());

        List<Descriptor> allSchedule2Descriptors = new ArrayList<>();
        List<String> physicalDisabilityAnswers = EsaActivityType.PHYSICAL_DISABILITIES.getAnswersExtractor().apply(caseData);
        if (physicalDisabilityAnswers != null) {
            List<Descriptor> physicalDisablityDescriptors = getEsaSchedule2DescriptorsFromQuestionKeys(caseData, physicalDisabilityAnswers);
            allSchedule2Descriptors.addAll(physicalDisablityDescriptors);
        }
        List<String> mentalAssessmentAnswers = EsaActivityType.MENTAL_ASSESSMENT.getAnswersExtractor().apply(caseData);
        if (mentalAssessmentAnswers != null) {
            List<Descriptor> mentalAssessmentDescriptors = getEsaSchedule2DescriptorsFromQuestionKeys(caseData, mentalAssessmentAnswers);
            allSchedule2Descriptors.addAll(mentalAssessmentDescriptors);
        }

        // Don't add descriptors to the template if the total points in schedule 2 are zero
        int totalPoints = allSchedule2Descriptors.stream().mapToInt(d -> d.getActivityAnswerPoints()).sum();
        if (totalPoints == 0) {
            allSchedule2Descriptors.clear();
        }

        if (allSchedule2Descriptors.isEmpty()) {
            if (caseData.isWcaAppeal()) {
                if (caseData.isSupportGroupOnlyAppeal()) {
                    builder.esaSchedule2Descriptors(null);
                    builder.esaNumberOfPoints(null);
                } else {
                    builder.esaSchedule2Descriptors(new ArrayList<>());
                    builder.esaNumberOfPoints(0);
                }
            } else {
                builder.esaSchedule2Descriptors(null);
                builder.esaNumberOfPoints(null);
            }
        } else {
            builder.esaSchedule2Descriptors(allSchedule2Descriptors);
            int numberOfPoints = allSchedule2Descriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum();
            if (EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(numberOfPoints)) {
                caseData.getSscsEsaCaseData().setDoesRegulation29Apply(null);
            }
            builder.esaNumberOfPoints(numberOfPoints);
        }
        if (caseData.getSscsEsaCaseData().getSchedule3Selections() != null && !caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty()) {
            builder.esaSchedule3Descriptors(getEsaSchedule3DescriptorsFromQuestionKeys(caseData, caseData.getSscsEsaCaseData().getSchedule3Selections()));
        }
        builder.regulation29Applicable(caseData.getSscsEsaCaseData().getDoesRegulation29Apply() == null ? null :  caseData.getSscsEsaCaseData().getDoesRegulation29Apply().toBoolean());
        builder.regulation35Applicable(caseData.getSscsEsaCaseData().getDoesRegulation35Apply() == null ? null :  caseData.getSscsEsaCaseData().getDoesRegulation35Apply().toBoolean());
        builder.supportGroupOnly(caseData.isSupportGroupOnlyAppeal());
    }


}
