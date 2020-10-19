package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import javax.validation.Validation;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipAwardType;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private WriteFinalDecisionMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new WriteFinalDecisionMidEventValidationHandler(Validation.buildDefaultValidatorFactory().getValidator());

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
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

    @Test
    public void givenAnEndDateIsBeforeStartDate_thenDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2019-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenADecisionDateIsInFuture_thenDisplayAnError() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(tomorrow.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice date of decision must not be in the future", error);
    }

    @Test
    public void givenADecisionDateIsToday_thenDoNotDisplayAnError() {

        LocalDate today = LocalDate.now();
        sscsCaseData.setWriteFinalDecisionDateOfDecision(today.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError() {

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDate(@Nullable String endDate) {
        sscsCaseData.setWriteFinalDecisionDateOfDecision(endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnEndDateIsSameAsStartDate_thenDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenAnEndDateIsAfterStartDate_thenDoNotDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    public void givenDailyLivingNoAwardAndDailyLivingHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living decision of No Award cannot be higher than DWP decision", error);
    }

    @Test
    public void givenMobilityNoAwardAndMobilityHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Mobility decision of No Award cannot be higher than DWP decision", error);
    }

    @Test
    public void givenDailyLivingEnhancedRateAndDailyLivingLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living award at Enhanced Rate cannot be lower than DWP decision", error);
    }

    @Test
    public void givenMobilityEnhancedRateAndMobilityLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("lower");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Mobility award at Enhanced Rate cannot be lower than DWP decision", error);
    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })

    public void givenDailyLivingValidAwardAndDwpComparison_thenDoNotDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
        "noAward, lower",
        "noAward, same",
    })

    public void givenDailyLivingValidAwardAndDwpComparisonWhenMobilityNotConsidered_thenDoNotDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })

    public void givenDailyLivingNotConsideredWhenMobilityNotConsidered_thenDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one of Mobility or Daily Living must be considered", error);
    }


    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })

    public void givenMobilityValidAwardAndDwpComparison_thenDoNotDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
        "noAward, lower",
        "noAward, same",
    })

    public void givenMobilityValidAwardAndDwpComparisonWhenDailyLivingNotConsidered_thenDoNotDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }


    @Test
    @Parameters({
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })

    public void givenMobilityNotConsideredWhenDailyLivingNotConsidered_thenDisplayAnError(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one of Mobility or Daily Living must be considered", error);
    }

    @Test
    @Parameters({
        "STANDARD_RATE, STANDARD_RATE",
        "ENHANCED_RATE, ENHANCED_RATE",
        "NOT_CONSIDERED, STANDARD_RATE",
        "STANDARD_RATE, NO_AWARD",
        "NO_AWARD, ENHANCED_RATE"
    })
    public void shouldDisplayActivitiesErrorWhenAnAnAwardIsGivenAndNoActivitiesSelected(PipAwardType dailyLiving, PipAwardType mobility) {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(dailyLiving.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(mobility.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one activity must be selected unless there is no award", error);
    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenActivitiesAreNotYetSelected() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
