package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.util.Iterator;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        sscsCaseData.setDwpState(FINAL_DECISION_ISSUED.getId());

        moveDraftFinalDecision(preSubmitCallbackResponse);

        clearTransientFields(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void moveDraftFinalDecision(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Iterator<SscsDocument> iterator = preSubmitCallbackResponse.getData().getSscsDocument().stream()
                .filter(e -> e.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).iterator();

        if (iterator.hasNext()) {
            while (iterator.hasNext()) {
                iterator.next().getValue().setDocumentType(DECISION_NOTICE.getValue());
            }
        } else {
            preSubmitCallbackResponse.addError("There is no Draft Decision Notice on the case so decision cannot be issued");
        }
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionPresentingAppellantAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        sscsCaseData.setWriteFinalDecisionStartDate(null);
        sscsCaseData.setWriteFinalDecisionEndDateType(null);
        sscsCaseData.setWriteFinalDecisionEndDate(null);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(null);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionTakingNutritionQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionManagingTherapyQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionWashAndBatheQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionManagingToiletNeedsQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionDressingAndUndressingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionCommunicatingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionReadingUnderstandingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionEngagingWithOthersQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionBudgetingDecisionsQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionPlanningAndFollowingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion(null);
        sscsCaseData.setWriteFinalDecisionReasonsForDecision(null);
        sscsCaseData.setWriteFinalDecisionPageSectionReference(null);
        sscsCaseData.setWriteFinalDecisionGenerateNotice(null);
        sscsCaseData.setWriteFinalDecisionPreviewDocument(null);
    }

}
