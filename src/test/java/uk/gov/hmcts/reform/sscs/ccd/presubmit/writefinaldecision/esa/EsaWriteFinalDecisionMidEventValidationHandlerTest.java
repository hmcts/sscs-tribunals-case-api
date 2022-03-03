package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static java.util.Objects.nonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public class EsaWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected String getBenefitType() {
        return "ESA";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation29Apply(NO);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
                Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        return new EsaWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData caseData) {

    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Collections.emptyList());
        caseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(Collections.emptyList());
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        caseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
    }

    @Test
    public void givenAnEmptySchedule3ActivitiesApplyFlag_thenSetTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YES, sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenActivitiesSpecified_thenDoNoDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenNullActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 3 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenEmptyActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 3 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenAnSchedule3ActivitiesApplyFlagSetToNo_thenDoNotChangeTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(NO, sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @Parameters({
            "Yes,No",
            "No,Yes",
            "null,No"
    })
    public void givenEsaCaseWithWcaAppealFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable String wcaFlow, String expectedShowResult) {

        sscsCaseData.setWcaAppeal(nonNull(wcaFlow) ?  isYesOrNo(wcaFlow) : null);
        System.out.println(sscsCaseData.getWcaAppeal());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("workCapabilityAssessment");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(isYesOrNo(expectedShowResult), response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    public void givenEsaCaseWithWcaAppealFlowAndNotOnWorkCapabilityAssessmentPage_thenDoNotSetShowSummaryOfOutcomePage() {

        sscsCaseData.setWcaAppeal(YES);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("somethingElse");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    @Parameters({
            "YES, allowed, YES",
            "YES, refused, NO",
            "NO, allowed, NO",
            "NO, refused, NO",
            "null, allowed, NO",
            "NO, null, NO",
            "null, null, NO",
    })
    public void givenEsaCaseWithWcaAppealFlowAndAllowedFlow_thenSetShowDwpReassessAwardPage(
            @Nullable String wcaFlow, @Nullable String allowedFlow, String expectedShowResult) {

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.setWcaAppeal(isYesOrNo(wcaFlow));
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(allowedFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("workCapabilityAssessment");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(isYesOrNo(expectedShowResult), response.getData().getShowDwpReassessAwardPage());
    }

    @Test
    public void givenEsaCaseWithWcaAppealFlowAndNotOnWorkCapabilityAssessmentPage_thenDoNotSetShowDwpReassessAwardPage() {

        sscsCaseData.setWcaAppeal(YES);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("somethingElse");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowDwpReassessAwardPage());
    }

    @Test
    @Parameters({"STANDARD_RATE, STANDARD_RATE",})
    @Override
    public void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {

        setValidPointsAndActivitiesScenario(sscsCaseData, YES.getValue());
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, YES.getValue());
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldNotDisplayErrorWhenNoAwardIsGivenAndEitherDailyLivingOrMobilityIsNotConsideredAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, YES.getValue());
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {

        setValidPointsAndActivitiesScenario(sscsCaseData, YES.getValue());
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {

        setValidPointsAndActivitiesScenario(sscsCaseData, YES.getValue());
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({"Yes, YES", "No, NO"})
    public void givenGenerateNoticeValueAndCaseIsEsa_thenShouldSetShowWorkCapabilityAssessment(String isGenerateNotice, @Nullable String showWorkCapabilityPage) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(isYesOrNo(isGenerateNotice));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(isYesOrNo(showWorkCapabilityPage), response.getData().getShowWorkCapabilityAssessmentPage());
    }

}
