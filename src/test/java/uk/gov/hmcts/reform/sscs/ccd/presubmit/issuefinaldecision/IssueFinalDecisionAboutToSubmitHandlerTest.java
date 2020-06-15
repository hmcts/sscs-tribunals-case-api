package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        handler = new IssueFinalDecisionAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        List<SscsDocument> documentList = new ArrayList<>();
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(documentList)
            .writeFinalDecisionTypeOfHearing("")
            .writeFinalDecisionPresentingOfficerAttendedQuestion("")
            .writeFinalDecisionAppellantAttendedQuestion("")
            .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
            .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
            .pipWriteFinalDecisionDailyLivingQuestion("")
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("")
            .pipWriteFinalDecisionMobilityQuestion("")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("")
            .writeFinalDecisionStartDate("")
            .writeFinalDecisionEndDateType("")
            .writeFinalDecisionEndDate("")
            .writeFinalDecisionDateOfDecision("")
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(new ArrayList<>())
            .pipWriteFinalDecisionMobilityActivitiesQuestion(new ArrayList<>())
            .pipWriteFinalDecisionPreparingFoodQuestion("")
            .pipWriteFinalDecisionTakingNutritionQuestion("")
            .pipWriteFinalDecisionManagingTherapyQuestion("")
            .pipWriteFinalDecisionWashAndBatheQuestion("")
            .pipWriteFinalDecisionManagingToiletNeedsQuestion("")
            .pipWriteFinalDecisionDressingAndUndressingQuestion("")
            .pipWriteFinalDecisionCommunicatingQuestion("")
            .pipWriteFinalDecisionReadingUnderstandingQuestion("")
            .pipWriteFinalDecisionEngagingWithOthersQuestion("")
            .pipWriteFinalDecisionBudgetingDecisionsQuestion("")
            .pipWriteFinalDecisionPlanningAndFollowingQuestion("")
            .pipWriteFinalDecisionMovingAroundQuestion("")
            .writeFinalDecisionReasonsForDecision("")
            .writeFinalDecisionPageSectionReference("")
            .writeFinalDecisionGenerateNotice("")
            .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_thenMoveDraftDocToSscsDocumentsAndClearTransientFields() {
        callback.getCaseDetails().getCaseData().getSscsDocument().add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());
        assertEquals(DECISION_NOTICE.getValue(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionReasonsForDecision());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsDocument().add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DECISION_NOTICE.getValue()).build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
