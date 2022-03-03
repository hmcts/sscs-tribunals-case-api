package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios.GenScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.*;

@Slf4j
@Component
public class GenWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    @Autowired
    public GenWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
                                                       GenDecisionNoticeQuestionService decisionNoticeQuestionService, GenDecisionNoticeOutcomeService decisionNoticeOutcomeService, DocumentConfiguration documentConfiguration,
                                                       VenueDataLoader venueDataLoader) {
        super(generateFile, userDetailsService, decisionNoticeQuestionService, decisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
    }

    @Override
    public String getBenefitType() {
        return "GEN";
    }

    @Override
    protected Descriptor buildDescriptorFromActivityAnswer(ActivityQuestion activityQuestion, ActivityAnswer answer) {
        return Descriptor.builder().build();
    }

    @Override
    protected void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData,
        WriteFinalDecisionTemplateBody payload) {

        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {

            Optional<GenAllowedOrRefusedCondition> condition = GenAllowedOrRefusedCondition.getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
            if (condition.isPresent()) {
                GenAllowedOrRefusedCondition allowedOrRefusedCondition = condition.get();
                GenScenario scenario = allowedOrRefusedCondition.getPipScenario();
                if (scenario != null) {
                    GenTemplateContent templateContent = scenario.getContent(payload);
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
        // No-op for GEN
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        builder.isDescriptorFlow(false);
    }
}
