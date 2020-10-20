package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipAwardType;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;

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

    @Mock
    private EsaDecisionNoticeQuestionService esaDecisionNoticeQuestionService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new WriteFinalDecisionMidEventValidationHandler(Validation.buildDefaultValidatorFactory().getValidator(), esaDecisionNoticeQuestionService);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

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

        LocalDate today = LocalDate.now();
        sscsCaseData.setWriteFinalDecisionDateOfDecision(today.toString());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError(String descriptorFlowValue) {

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(yesterday.toString());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateDescriptorFlow(@Nullable String endDate) {
        sscsCaseData.setWriteFinalDecisionDateOfDecision(endDate);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDateNonDescriptorFlow(@Nullable String endDate) {
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
        sscsCaseData.setWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters(named = "descriptorFlowValues")
    public void givenANonPdfDecisionNotice_thenDisplayAnError(String descriptorFlowValue) {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    public void givenDailyLivingNoAwardAndDailyLivingHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living decision of No Award cannot be higher than DWP decision", error);
    }

    @Test
    public void givenMobilityNoAwardAndMobilityHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Mobility decision of No Award cannot be higher than DWP decision", error);
    }

    @Test
    public void givenDailyLivingEnhancedRateAndDailyLivingLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living award at Enhanced Rate cannot be lower than DWP decision", error);
    }

    @Test
    public void givenMobilityEnhancedRateAndMobilityLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("lower");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

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

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }
    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsNA_thenDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsNA_thenDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());


        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());


        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsIndefinite_theDoNotDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsIndefinite_theDoNotDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsIndefinite_thenDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);
        
        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsIndefinite_thenDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsSetEndDate_theDoNotDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsSetEndDate_theDoNotDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenEndDateTypeIsSetEndDate_thenDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenEndDateTypeIsSetEndDate_thenDisplayAnErrorForNoAwardsAndNotConsidered(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }

    }


    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenMobilityNotConsideredWhenEndDateTypeIsNA_thenDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());


    }


    @Test
    @Parameters({
        "enhancedRate, same",
        "enhancedRate, higher",
        "standardRate, same",
        "standardRate, lower",
        "standardRate, higher",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenDailyLivingNotConsideredWhenEndDateTypeIsNA_thenDisplayAnErrorForAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);
        
        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());


    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenMobilityNotConsideredWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenDailyLivingNotConsideredWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwards(String award, String comparison) {

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }

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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

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

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }
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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getWriteFinalDecisionEndDateType());
        }

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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

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
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsNA() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals("na", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldDisplayActivitiesErrorWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());
        assertEquals(0, response.getWarnings().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldDisplayActivitiesErrorWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.NO_AWARD.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());
        assertEquals(0, response.getWarnings().size());

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenActivitiesAreNotYetSelected() {
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(PipAwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(PipAwardType.STANDARD_RATE.getKey());
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("Yes");

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
