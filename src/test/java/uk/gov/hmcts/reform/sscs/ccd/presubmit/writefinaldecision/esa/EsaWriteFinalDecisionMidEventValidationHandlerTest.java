package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
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
}
