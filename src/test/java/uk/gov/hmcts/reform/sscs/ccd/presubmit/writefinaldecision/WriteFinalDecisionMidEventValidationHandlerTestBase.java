package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import java.util.Iterator;
import javax.validation.Validation;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public abstract class WriteFinalDecisionMidEventValidationHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected WriteFinalDecisionMidEventValidationHandler handler;

    protected abstract String getBenefitType();

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    @Mock
    protected IdamClient idamClient;

    @Mock
    protected UserDetails userDetails;

    protected SscsCaseData sscsCaseData;

    protected abstract void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue);

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
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2019-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsInFuture_thenDisplayAnError(String descriptorFlowValue) {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(tomorrow.toString());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

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
        sscsCaseData.setWriteFinalDecisionDateOfDecision(today.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError(String descriptorFlowValue) {


        setValidPointsAndActivitiesScenario(sscsCaseData, descriptorFlowValue);

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateDescriptorFlow(@Nullable String endDate) {

        setValidPointsAndActivitiesScenario(sscsCaseData, null);

        sscsCaseData.setWriteFinalDecisionDateOfDecision(endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateNonDescriptorFlow(@Nullable String endDate) {

        setValidPointsAndActivitiesScenario(sscsCaseData, null);

        sscsCaseData.setWriteFinalDecisionDateOfDecision(endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenAnEndDateIsSameAsStartDate_thenDisplayAnError(String descriptorFlowValue) {
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenAnEndDateIsAfterStartDate_thenDoNotDisplayAnError(String descriptorFlowValue) {

        setValidPointsAndActivitiesScenario(sscsCaseData, descriptorFlowValue);

        sscsCaseData.setWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenANonPdfDecisionNotice_thenDisplayAnError(String descriptorFlowValue) {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    @Parameters({
        "STANDARD_RATE, STANDARD_RATE",
        "ENHANCED_RATE, ENHANCED_RATE",
        "NOT_CONSIDERED, STANDARD_RATE",
        "STANDARD_RATE, NO_AWARD",
        "NO_AWARD, ENHANCED_RATE"
    })
    public void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(dailyLiving.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(mobility.getKey());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());

        if ("PIP".equals(getBenefitType())) {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("At least one activity must be selected unless there is no award", error);
        } else {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("At least one activity must be selected.", error);
        }
    }

    protected abstract void setNoAwardsScenario(SscsCaseData caseData);

    protected abstract void setEmptyActivitiesListScenario(SscsCaseData caseData);

    protected abstract void setNullActivitiesListScenario(SscsCaseData caseData);


    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        if ("PIP".equals(getBenefitType())) {
            assertEquals(0, response.getErrors().size());
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("At least one activity must be selected.", error);
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsNA() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Iterator<String> iterator = response.getErrors().iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }

        assertEquals(0, response.getWarnings().size());

        if ("PIP".equals(getBenefitType())) {
            assertEquals(0, response.getErrors().size());
        } else {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("At least one activity must be selected.", error);
        }

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        if ("PIP".equals(getBenefitType())) {
            assertEquals(1, response.getErrors().size());
            assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());
            assertEquals(0, response.getWarnings().size());
        } else {
            assertEquals(1, response.getErrors().size());
            assertEquals("At least one activity must be selected.", response.getErrors().iterator().next());
            assertEquals(0, response.getWarnings().size());
        }
        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldDisplayActivitiesErrorForPipOnlyWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        if ("PIP".equals(getBenefitType())) {
            assertEquals(1, response.getErrors().size());
            assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());
        } else {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("At least one activity must be selected.", error);
        }

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenActivitiesAreNotYetSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(AwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(AwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
