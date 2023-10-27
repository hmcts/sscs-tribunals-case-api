package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase.CANT_UPLOAD_ERROR_MESSAGE;

import java.time.LocalDate;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public abstract class WriteFinalDecisionMidEventValidationHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static WriteFinalDecisionMidEventValidationHandlerBase handler;

    protected abstract String getBenefitType();

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    @Mock
    protected IdamClient idamClient;

    @Mock
    protected UserDetails userDetails;

    @Mock
    protected UserInfo userInfo;


    @Mock
    protected DecisionNoticeService decisionNoticeService;

    @Mock
    protected DecisionNoticeQuestionService decisionNoticeQuestionService;

    protected SscsCaseData sscsCaseData;

    protected abstract void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue);

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    protected abstract WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService, boolean isPostHearingsEnabled);

    @Before
    public void setUp() {
        openMocks(this);

        handler = createValidationHandler(validator, decisionNoticeService, false);

        when(decisionNoticeService.getQuestionService(getBenefitType())).thenReturn(decisionNoticeQuestionService);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserInfo("Bearer token")).thenReturn(userInfo);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(getBenefitType()).build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @NamedParameters("descriptorFlowValues")
    @SuppressWarnings("unused")
    private Object[] descriptorFlowValues() {
        return new Object[]{
            new String[]{null},
            new String[]{"Yes"}
        };
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenAnEndDateIsBeforeStartDate_thenDisplayAnError(String descriptorFlowValue) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate("2019-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsInFuture_thenDisplayAnError(String descriptorFlowValue) {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(tomorrow.toString());
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice date of decision must not be in the future", error);
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsToday_thenDoNotDisplayAnError(String descriptorFlowValue) {

        setValidPointsAndActivitiesScenario(sscsCaseData, descriptorFlowValue);

        LocalDate today = LocalDate.now();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(today.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError(String descriptorFlowValue) {


        setValidPointsAndActivitiesScenario(sscsCaseData, descriptorFlowValue);

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateDescriptorFlow(@Nullable String endDate) {

        setValidPointsAndActivitiesScenario(sscsCaseData, null);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateNonDescriptorFlow(@Nullable String endDate) {

        setValidPointsAndActivitiesScenario(sscsCaseData, null);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenAnEndDateIsSameAsStartDate_thenDisplayAnError(String descriptorFlowValue) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate("2020-01-01");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenAnEndDateIsAfterStartDate_thenDoNotDisplayAnError(String descriptorFlowValue) {

        setValidPointsAndActivitiesScenario(sscsCaseData, descriptorFlowValue);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenANonPdfDecisionNotice_thenDisplayAnError(String descriptorFlowValue) {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    @Parameters({"typeOfAppeal", "previewDecisionNotice"})
    public void givenDeathOfAppellant_thenDisplayWarning(String pageId) {
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(callback.isIgnoreWarnings()).thenReturn(false);
        when(callback.getPageId()).thenReturn(pageId);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream().findFirst().orElse("");
        assertThat(warning, is("Appellant is deceased. Copy the draft decision and amend offline, then upload the offline version."));
    }

    @Test
    public void givenDeathOfAppellantButNotOnTheCorrectPage_thenDoNotDisplayWarning() {
        when(callback.getPageId()).thenReturn("incorrectPage");
        when(callback.isIgnoreWarnings()).thenReturn(false);
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenDeathOfAppellantWithIgnoreWarnings_thenDoNotDisplayWarning() {
        when(callback.isIgnoreWarnings()).thenReturn(true);
        when(callback.getPageId()).thenReturn("typeOfAppeal");
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenNoDeathOfAppellant_thenDoNotDisplayWarning() {
        when(callback.getPageId()).thenReturn("typeOfAppeal");
        sscsCaseData.setIsAppellantDeceased(YesNo.NO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    protected abstract void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility);

    protected abstract void setNoAwardsScenario(SscsCaseData caseData);

    protected abstract void setEmptyActivitiesListScenario(SscsCaseData caseData);

    protected abstract void setNullActivitiesListScenario(SscsCaseData caseData);

    protected abstract void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected();

    protected abstract void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate();

    protected abstract void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite();

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsNA() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());

        assertEquals(0, response.getErrors().size());

        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void whenCorrectionIsInProgressDecisionWasUploadedGenerateNoticeIsYes_thenThrowError() {
        handler = createValidationHandler(validator, decisionNoticeService, true);

        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YesNo.YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionWasOriginalDecisionUploaded(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(CANT_UPLOAD_ERROR_MESSAGE));
    }

    @Test
    public void whenCorrectionIsInProgressDecisionWasGeneratedGenerateNoticeIsYes_thenDontThrowError() {
        handler = createValidationHandler(validator, decisionNoticeService, true);

        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YesNo.NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionWasOriginalDecisionUploaded(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void whenCorrectionIsInProgressDecisionWasUploadedGenerateNoticeIsNo_thenDontThrowError() {
        handler = createValidationHandler(validator, decisionNoticeService, true);

        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YesNo.YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionWasOriginalDecisionUploaded(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
