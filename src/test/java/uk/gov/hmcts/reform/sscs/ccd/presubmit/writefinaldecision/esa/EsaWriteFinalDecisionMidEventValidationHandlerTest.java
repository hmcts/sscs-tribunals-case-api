package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

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
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
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

        assertEquals("Yes", sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
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
            "Yes, NO",
            "No,YES",
            "null, NO"
    })
    public void givenEsaCaseWithWcaAppealFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable String wcaFlow, YesNo expectedShowResult) {

        sscsCaseData.setWcaAppeal(wcaFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
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

        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

}
