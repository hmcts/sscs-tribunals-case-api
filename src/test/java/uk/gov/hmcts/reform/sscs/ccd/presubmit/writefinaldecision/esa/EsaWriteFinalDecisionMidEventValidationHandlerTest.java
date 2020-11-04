package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;

@RunWith(JUnitParamsRunner.class)
public class  EsaWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected String getBenefitType() {
        return "ESA";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
                Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData caseData) {

    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        caseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Collections.emptyList());
        caseData.setEsaWriteFinalDecisionMentalAssessmentQuestion(Collections.emptyList());
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        caseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        caseData.setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
    }

    @Test
    public void givenAnEmptySchedule3ActivitiesApplyFlag_thenSetTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertNull(sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("Yes", sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenAnSchedule3ActivitiesApplyFlagSetToNo_thenDoNotChangeTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("No", sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesApply());

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
}
