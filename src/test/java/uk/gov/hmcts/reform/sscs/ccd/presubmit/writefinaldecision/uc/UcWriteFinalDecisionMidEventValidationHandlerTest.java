package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ElementDisputed;
import uk.gov.hmcts.reform.sscs.ccd.domain.ElementDisputedDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public class UcWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected String getBenefitType() {
        return "UC";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(
                Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService, boolean isPostHearingsEnabled) {
        return new UcWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, isPostHearingsEnabled);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData caseData) {

    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Collections.emptyList());
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(Collections.emptyList());
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
    }

    @Test
    public void givenAnEmptySchedule7ActivitiesApplyFlag_thenSetTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("Yes", sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenSchedule7ActivitiesApplyFlagSetToYes_WhenActivitiesSpecified_thenDoNoDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule7ActivitiesApplyFlagSetToYes_WhenNullActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 7 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenSchedule7ActivitiesApplyFlagSetToYes_WhenEmptyActivities_thenDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please select the Schedule 7 Activities that apply, or indicate that none apply", response.getErrors().iterator().next());
    }

    @Test
    public void givenAnSchedule7ActivitiesApplyFlagSetToNo_thenDoNotChangeTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("No", sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @Parameters({
        "YES, NO",
        "NO,YES",
        "null, NO"
    })
    public void givenUcCaseWithWcaAppealFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable YesNo wcaFlow, YesNo expectedShowResult) {

        sscsCaseData.setWcaAppeal(wcaFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("workCapabilityAssessment");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    public void givenUcCaseWithWcaAppealFlowAndNotOnWorkCapabilityAssessmentPage_thenDoNotSetShowSummaryOfOutcomePageOrShowShowDwpReassessAwardPage() {

        sscsCaseData.setWcaAppeal(YES);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getPageId()).thenReturn("somethingElse");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    @Parameters({
        "YES, allowed, YES",
        "NO, refused, NO",
        "NO, allowed, NO",
        "NO, refused, NO",
        "null, allowed, NO",
        "NO, null, NO",
        "null, null, NO",
    })
    public void givenUcCaseWithWcaAppealFlowAndAllowedFlow_thenSetShowDwpReassessAwardPage(
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
    public void givenUcCaseWithWcaAppealFlowAndNotOnWorkCapabilityAssessmentPage_thenDoNotSetShowDwpReassessAwardPage() {

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
    public void givenGenerateNoticeValueAndCaseIsUc_thenShouldSetShowWorkCapabilityAssessment(YesNo isGenerateNotice, @Nullable YesNo showWorkCapabilityPage) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(isGenerateNotice);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(showWorkCapabilityPage, response.getData().getShowWorkCapabilityAssessmentPage());
    }

    @Test
    @Parameters({"SV, YES", "DD, NO"})
    public void shouldSetHasSvIssueCode(String issueCode, YesNo expectedHasSvIssueCode) {
        UcWriteFinalDecisionMidEventValidationHandler handlerWithSevereConditions = new UcWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, true);

        sscsCaseData.setElementsDisputedLimitedWork(List.of(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode(issueCode).build()).build()));
        handlerWithSevereConditions.setDefaultFields(sscsCaseData);

        assertThat(expectedHasSvIssueCode).isEqualTo(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionHasSVIssueCode());
    }

    @Test
    @Parameters({"NO, Yes", "YES, No"})
    public void givenSchedule7DoesNotApplyOrShouldNotBeShown_thenShouldClearSchedule7Activities(YesNo showSchedule7ActivitiesPage, String ucWriteFinalDecisionSchedule7ActivitiesApply) {
        sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(showSchedule7ActivitiesPage);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(ucWriteFinalDecisionSchedule7ActivitiesApply);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(List.of("schedule7MobilisingUnaided"));
        UcWriteFinalDecisionMidEventValidationHandler handlerWithSevereConditions = new UcWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, true);
        handlerWithSevereConditions.setDefaultFields(sscsCaseData);

        assertThat(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion()).isNull();
    }
}
