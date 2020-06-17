package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public IssueFinalDecisionAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

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

        createFinalDecisionNoticeFromDraft(preSubmitCallbackResponse);

        clearTransientFields(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void createFinalDecisionNoticeFromDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Iterator<SscsDocument> iterator = preSubmitCallbackResponse.getData().getSscsDocument().stream()
                .filter(e -> e.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).iterator();

        if (iterator.hasNext()) {
            while (iterator.hasNext()) {
                SscsDocument sscsDocument = iterator.next();
                String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"));

                DocumentLink documentLink = DocumentLink.builder()
                    .documentUrl(sscsDocument.getValue().getDocumentLink().getDocumentUrl())
                    .documentFilename(DocumentType.DECISION_NOTICE.getValue() + " issued on " + now + ".pdf")
                    .documentBinaryUrl(sscsDocument.getValue().getDocumentLink().getDocumentBinaryUrl())
                    .build();

                footerService.createFooterAndAddDocToCase(documentLink, preSubmitCallbackResponse.getData(), DocumentType.DECISION_NOTICE, now);
            }

        } else {
            preSubmitCallbackResponse.addError("There is no Draft Decision Notice on the case so decision cannot be issued");
        }

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionAppellantAttendedQuestion(null);
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
        sscsCaseData.setWriteFinalDecisionPreviewDocument(null);
    }

}
