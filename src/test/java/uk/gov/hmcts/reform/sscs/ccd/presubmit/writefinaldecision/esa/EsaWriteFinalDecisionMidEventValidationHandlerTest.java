package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
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
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
                Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService, boolean isPostHearingsEnabled) {
        return new EsaWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, isPostHearingsEnabled);
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

        assertEquals("Yes", sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenActivitiesSpecified_thenDoNoDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenNullActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 3 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenSchedule3ActivitiesApplyFlagSetToYes_WhenEmptyActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 3 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenAnSchedule3ActivitiesApplyFlagSetToNo_thenDoNotChangeTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("No", sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @Parameters({
        "YES, NO",
        "NO,YES",
        "null, NO"
    })
    public void givenEsaCaseWithWcaAppealFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable YesNo wcaFlow, YesNo expectedShowResult) {

        sscsCaseData.setWcaAppeal(wcaFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("workCapabilityAssessment");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
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
            @Nullable YesNo wcaFlow, @Nullable String allowedFlow, YesNo expectedShowResult) {

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.setWcaAppeal(wcaFlow);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(allowedFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("workCapabilityAssessment");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowDwpReassessAwardPage());
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

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldNotDisplayErrorWhenNoAwardIsGivenAndEitherDailyLivingOrMobilityIsNotConsideredAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
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

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    @Parameters({"YES, YES", "NO, NO"})
    public void givenGenerateNoticeValueAndCaseIsEsa_thenShouldSetShowWorkCapabilityAssessment(YesNo isGenerateNotice, @Nullable YesNo showWorkCapabilityPage) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(isGenerateNotice);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(showWorkCapabilityPage, response.getData().getShowWorkCapabilityAssessmentPage());
    }

}
