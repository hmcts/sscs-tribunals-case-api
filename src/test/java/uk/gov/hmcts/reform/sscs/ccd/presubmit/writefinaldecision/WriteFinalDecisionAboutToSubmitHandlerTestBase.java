package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.*;

public abstract class WriteFinalDecisionAboutToSubmitHandlerTestBase<T extends DecisionNoticeQuestionService> {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected WriteFinalDecisionAboutToSubmitHandler handler;

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;
    @Mock
    protected UserDetailsService userDetailsService;

    protected T decisionNoticeQuestionService;
    protected DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    protected DecisionNoticeService decisionNoticeService;
    protected PreviewDocumentService previewDocumentService;
    protected SscsCaseData sscsCaseData;

    protected abstract DecisionNoticeOutcomeService createOutcomeService(T decisionNoticeQuestionService);

    public WriteFinalDecisionAboutToSubmitHandlerTestBase(T decisionNoticeQuestionService) {
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
        this.decisionNoticeOutcomeService = createOutcomeService(decisionNoticeQuestionService);
    }

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        decisionNoticeService = new DecisionNoticeService(Arrays.asList(decisionNoticeQuestionService), Arrays.asList(createOutcomeService(decisionNoticeQuestionService)), Arrays.asList());
        previewDocumentService = new PreviewDocumentService();
        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeService, previewDocumentService, userDetailsService, false);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(decisionNoticeQuestionService.getBenefitType()).build()).build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }


    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in WriteFinalDecisionAboutToSubmitHandler to set
     * the generated date to now, even though it is already being determined by the
     * preview document handler.  This is because on submission, the correct generated date
     * (the one referenced in the preview document) is being overwritten to a null value.
     * Once RDM-8200 is fixed and we remove the workaround, this test should be changed
     * to assert that a "something has gone wrong" error is displayed, as a null generated
     * date would indicate that the date in the preview document hasn't been set.
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateNotSet_thenSetGeneratedDateAsNowAndDoNotDisplayAnError() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(LocalDate.now().toString(), sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());

    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the WriterFinalSubmissionAboutToSubmitHandler,
     * then that date is updated to now() after the WriterFinalSubmissionAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the WriterFinalSubmissionAboutToSubmitHandler
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateSet_thenSetUpdateGeneratedDateAndDoNotDisplayAnError() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate("2018-01-01");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(LocalDate.now().toString(), sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
    }

    @Test
    public void givenEndDateTypeOfIndefinite_thenDoNotSetEndDateTypeToNull() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");
        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("indefinite", sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void givenEndDateTypeOfSetEndDate_thenDoNotSetEndDateTypeToNull() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("setEndDate", sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    protected abstract void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue);

    @Test
    public void givenEndDateTypeOfNA_thenSetEndDateTypeToNull() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void givenWriteFinalDecisionPostHearingsEnabledAndNoIssueFinalDate_shouldUpdateFinalCaseData() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        sscsCaseData.getDocumentGeneration().setSignedBy("name");
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn("surname");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionIdamSurname(), "surname");
        assertEquals(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionGeneratedDate(), LocalDate.now());
    }

    @Test
    public void givenWriteFinalDecisionPostHearingsEnabledAndNoNullIssueFinalDate_shouldNotUpdateFinalCaseData() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionIssuedDate(LocalDate.now());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionIdamSurname());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionGeneratedDate());
    }

    @Test
    public abstract void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft();

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
