package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
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
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private WriteFinalDecisionAboutToStartHandler handler;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
  
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        sscsCaseData = new SscsCaseData();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);

        sscsCaseData.setSscsDocument(List.of(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("doctype")
                .build())
            .build()));
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))).build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionTypeOfHearing("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPresentingOfficerAttendedQuestion("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppellantAttendedQuestion("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName("");
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAwarenessOfHazardsQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCommunicationQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionConsciousnessQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCopingWithChangeQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionGettingAboutQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLearningTasksQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLossOfControlQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMakingSelfUnderstoodQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionManualDexterityQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(new LinkedList<>());
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(new LinkedList<>());
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionNavigationQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPersonalActionQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPickingUpQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionReachingQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new LinkedList<>());
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSocialEngagementQuestion("");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionStandingAndSittingQuestion("");
        sscsCaseData.setDwpReassessTheAward("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(new LinkedList<>());
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPageSectionReference("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("");
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("");

        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, false);
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenNullCaseDetails_thenReturnFalse() {
        when(callback.getCaseDetails()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenNullCaseData_thenReturnFalse() {
        when(caseDetails.getCaseData()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenPostHearingsFalse_thenErrorsShouldBeEmpty() {
        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    @Parameters({"POST_HEARING", "DORMANT_APPEAL_STATE"})
    public void givenStateIsDormantOrPostHearingsAndIsSalariedJudge_thenErrorsShouldBeEmpty(State state) {
        when(caseDetails.getState()).thenReturn(state);
        when(userDetailsService.getUserRoles(USER_AUTHORISATION)).thenReturn(List.of(UserRole.SALARIED_JUDGE.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    public void givenStateIsNorDormantOrPostHearings_thenErrorsShouldBeEmpty() {
        when(caseDetails.getState()).thenReturn(State.VALID_APPEAL);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
      
        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    public void givenStateIsDormantAndIsntSalariedJudge_thenThrowError() {
        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, true);
        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        when(userDetailsService.getUserRoles(USER_AUTHORISATION)).thenReturn(List.of(UserRole.JUDGE.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have already issued a final decision, only a salaried Judge can correct it", error);
    }

    @Test
    @Parameters({"POST_HEARING", "DORMANT_APPEAL_STATE"})
    public void givenAWriteFinalDecisionEventForCorrectionWithPostHearingsEnabled_thenKeepData(State state) {
        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, true);
        when(caseDetails.getState()).thenReturn(state);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertDataRetained(response);
    }

    @Test
    public void givenAWriteFinalDecisionEventNotForCorrectionWithPostHearingsEnabled_thenDeleteData() {
        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, true);
        when(caseDetails.getState()).thenReturn(State.VALID_APPEAL);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertDataDeleted(response);
    }

    @Test
    public void givenAWriteFinalDecisionEventForCorrectionWithPostHearingsNotEnabled_thenDeleteData() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertDataDeleted(response);
    }

    @Test
    public void givenAWriteFinalDecisionEventWithDraftDocumentOnWithPostHearingsNotEnabled_thenDeleteData() {
        sscsCaseData.setSscsDocument(List.of(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType(DRAFT_DECISION_NOTICE.getValue())
                        .build())
                .build()));
        sscsCaseData.setState(State.VALID_APPEAL);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertDataRetained(response);
    }

    private void assertDataRetained(PreSubmitCallbackResponse<SscsCaseData> response) {
        assertEquals(0, response.getErrors().size());

        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getDwpReassessTheAward());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule9Paragraph4Apply());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    private void assertDataDeleted(PreSubmitCallbackResponse<SscsCaseData> response) {
        assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getDwpReassessTheAward());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule9Paragraph4Apply());
        assertNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }
}
