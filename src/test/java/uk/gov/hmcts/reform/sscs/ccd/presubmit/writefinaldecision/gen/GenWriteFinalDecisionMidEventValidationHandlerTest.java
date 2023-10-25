package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Arrays;
import java.util.List;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;


@RunWith(JUnitParamsRunner.class)
public class GenWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService, boolean isPostHearingsEnabled) {
        return new GenWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService, isPostHearingsEnabled);
    }

    @Override
    protected String getBenefitType() {
        return "GEN";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData sscsCaseData) {
        // N/A for GEN
    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        // N/A for GEN
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {
        // N/A for GEN
    }


    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {
        // N/A for GEN
    }

    @Test
    public void givenGenerateNoticeValueAndCaseIsGen_thenDoNotSetShowWorkCapabilityAssessment() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowWorkCapabilityAssessmentPage());
    }

    @Test
    public void givenGenerateNoticeValueAndCaseIsGenAndHasOtherParties_thenShowOtherPartiesAttendedHearing() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setOtherParties(otherParties);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getOtherPartyAttendedQuestions());
        assertTrue(response.getData().getSscsFinalDecisionCaseData().getOtherPartyAttendedQuestions().size() > 0);
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(Name.builder().firstName("Harry").lastName("Kane").build())
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).name(Name.builder().firstName("Henry").lastName("Smith").build()).build())
                        .rep(Representative.builder().id(repId).name(Name.builder().firstName("Wendy").lastName("Smith").build()).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }


}

