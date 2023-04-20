package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipPointsCondition;

@Slf4j
@Service
public class DecisionNoticeService {

    private List<DecisionNoticeQuestionService> decisionNoticeQuestionServices;
    private List<DecisionNoticeOutcomeService> decisionNoticeOutcomeServices;
    private List<WriteFinalDecisionPreviewDecisionServiceBase> previewDecisionServices;

    @Autowired
    public DecisionNoticeService(List<DecisionNoticeQuestionService> decisionNoticeQuestionServices, List<DecisionNoticeOutcomeService> decisionNoticeOutcomeServices,
        List<WriteFinalDecisionPreviewDecisionServiceBase> previewDecisionServices) {
        this.decisionNoticeQuestionServices = decisionNoticeQuestionServices;
        this.decisionNoticeOutcomeServices = decisionNoticeOutcomeServices;
        this.previewDecisionServices = previewDecisionServices;
    }

    public DecisionNoticeQuestionService getQuestionService(String benefitType) {
        Optional<DecisionNoticeQuestionService> matchingService = decisionNoticeQuestionServices.stream().filter(s -> s.getBenefitType().equals(benefitType)).findFirst();

        if (matchingService.isPresent()) {
            return matchingService.get();
        } else {
            throw new IllegalStateException("No question service registered for benefit type:" + benefitType);
        }
    }

    public WriteFinalDecisionPreviewDecisionServiceBase getPreviewService(String benefitType) {
        Optional<WriteFinalDecisionPreviewDecisionServiceBase> matchingService = previewDecisionServices.stream().filter(s -> s.getBenefitType().equals(benefitType)).findFirst();

        if (matchingService.isPresent()) {
            return matchingService.get();
        } else {
            throw new IllegalStateException("No question service registered for benefit type:" + benefitType);
        }
    }

    public DecisionNoticeOutcomeService getOutcomeService(String benefitType) {
        Optional<DecisionNoticeOutcomeService> matchingService = decisionNoticeOutcomeServices.stream().filter(s -> s.getBenefitType().equals(benefitType)).findFirst();

        if (matchingService.isPresent()) {
            return matchingService.get();
        } else {
            throw new IllegalStateException("No outcome service registered for benefit type:" + benefitType);
        }
    }

    public Class<? extends PointsCondition<?>> getPointsConditionEnumClass(String benefitType) {
        return PipPointsCondition.class;
    }

    public void handleFinalDecisionNotice(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, PreviewDocumentService previewDocumentService) {
        State state = sscsCaseData.getState();
        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);
        if (benefitType == null) {
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
        } else {
            DecisionNoticeOutcomeService outcomeService = getOutcomeService(benefitType);
            outcomeService.validate(preSubmitCallbackResponse, sscsCaseData);
            if (!(State.READY_TO_LIST.equals(state) || State.WITH_DWP.equals(state))) {
                sscsCaseData.setPreviousState(state);
            }
            previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_DECISION_NOTICE, sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        }
    }
}
