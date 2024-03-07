package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public class PipWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Test
    public void givenDailyLivingNoAwardAndDailyLivingHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living decision of No Award cannot be higher than DWP decision", error);
    }

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService, boolean isPostHearingsEnabled) {
        return new PipWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, isPostHearingsEnabled);
    }

    @Test
    public void givenMobilityNoAwardAndMobilityHigherThanDwp_thenDisplayAnError() {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Mobility decision of No Award cannot be higher than DWP decision", error);
    }

    @Test
    public void givenDailyLivingEnhancedRateAndDailyLivingLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Daily living award at Enhanced Rate cannot be lower than DWP decision", error);
    }

    @Test
    public void givenMobilityEnhancedRateAndMobilityLowerThanDwp_thenDisplayAnError() {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("lower");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());


        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());


        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", error);

        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());


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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("An end date must be provided or set to Indefinite for this decision.", error);

        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());


    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
    })
    public void givenDailyLivingValidAwardAndDwpComparisonWhenMobilityNotConsideredWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwards(String award, String comparison) {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        }

    }

    @Test
    @Parameters({
        "noAward, lower",
        "noAward, same",
    })
    public void givenMobilityValidAwardAndDwpComparisonWhenDailyLivingNotConsideredWhenEndDateTypeIsNA_thenDoNotDisplayAnErrorForNoAwards(String award, String comparison) {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        }

    }

    @Test
    @Parameters({
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })
    public void givenDailyLivingNotConsideredWhenMobilityNotConsidered_thenDisplayAnError(String award, String comparison) {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
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

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        if ("noAward".equals(award) || "notConsidered".equals(award)) {
            assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        } else {
            assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        }
    }

    @Test
    @Parameters({
        "notConsidered, lower",
        "notConsidered, higher",
        "notConsidered, same",
    })

    public void givenMobilityNotConsideredWhenDailyLivingNotConsidered_thenDisplayAnError(String award, String comparison) {

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(award);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparison);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one of Mobility or Daily Living must be considered", error);
    }

    @Test
    @Parameters({
        "Yes, YES, NO",
        "Yes, NO, NO",
        "No, YES, YES",
        "No, NO, NO",
        "null, NO, NO",
        "No, null, NO",
    })
    public void givenPipCaseWithDescriptorFlowAndGenerateNoticeFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable String descriptorFlow, @Nullable YesNo generateNoticeFlow, YesNo expectedShowResult) {

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlow);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(generateNoticeFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    public void givenPipCase_thenDoNotShowDwpReassessAwardPage() {

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("Yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getShowDwpReassessAwardPage());
    }

    @Test
    @Parameters({
        "STANDARD_RATE, STANDARD_RATE",
        "ENHANCED_RATE, ENHANCED_RATE",
        "NOT_CONSIDERED, STANDARD_RATE",
        "STANDARD_RATE, NO_AWARD",
        "NO_AWARD, ENHANCED_RATE"
    })
    @Override
    public void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(dailyLiving.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(mobility.getKey());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one activity must be selected unless there is no award", error);
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardAndNotConsideredAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(AwardType.NOT_CONSIDERED.getKey());
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNotConsideredAndNoAwardAreGivenAndNoActivitiesAreSelected3() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(AwardType.NOT_CONSIDERED.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(AwardType.NO_AWARD.getKey());
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
        assertEquals("na", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void shouldNotDisplayActivitiesErrorWhenActivitiesAreNotYetSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(AwardType.STANDARD_RATE.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(AwardType.STANDARD_RATE.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertNull(caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());
        assertEquals(0, response.getWarnings().size());
        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());
        assertEquals("End date is not applicable for this decision - please specify 'N/A - No Award'.", response.getErrors().iterator().next());

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
    }

    @Test
    public void givenGenerateNoticeValueAndCaseIsPip_thenDoNotSetShowWorkCapabilityAssessment() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowWorkCapabilityAssessmentPage());
    }

    @Override
    protected String getBenefitType() {
        return "PIP";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(AwardType.NO_AWARD.getKey());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(AwardType.NO_AWARD.getKey());
    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(emptyList());
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(emptyList());
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
    }

}

