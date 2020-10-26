package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class EsaWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase {

    public EsaWriteFinalDecisionAboutToSubmitHandlerTest() throws IOException {
        super(new EsaDecisionNoticeQuestionService());
    }

    @NamedParameters("schedule3ActivityAndRegulation35Combinations")
    @SuppressWarnings("unused")
    private Object[] schedule3ActivityAndRegulation35Combinations() {
        return new Object[]{
            new Boolean[]{null, null},
            new Boolean[]{false, null},
            new Boolean[]{true, null},
            new Boolean[]{null, false},
            new Boolean[]{false, false},
            new Boolean[]{true, false},
            new Boolean[]{null, true},
            new Boolean[]{false, true},
            new Boolean[]{true, true},
        };
    }

    @NamedParameters("schedule3ActivityCombinations")
    @SuppressWarnings("unused")
    private Object[] schedule3ActivityCombinations() {
        return new Object[]{
            new Boolean[]{null},
            new Boolean[]{false},
            new Boolean[]{true},
        };
    }

    @Test
    @Parameters(named = "schedule3ActivityAndRegulation35Combinations")
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreTooHigh_thenDisplayAnError(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);

        if (schedule3Activities != null) {
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        if (schedule3Activities == null) {
            if (regulation35 == null) {
                assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                }
            }
        } else if (!schedule3Activities.booleanValue()) {
            if (regulation35 == null) {
                assertEquals(
                    "You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and made no selections for the Schedule 3 Activities question. Please review your previous selection.",
                    error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
                }
            }
        } else {
            if (regulation35 == null) {
                assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question and made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question and made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
                }
            }
        }
    }

    @Test
    @Parameters(named = "schedule3ActivityAndRegulation35Combinations")
    public void givenRegulation29FieldIsPopulatedWithNoAndPointsAreTooHigh_thenDisplayAnError(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        if (schedule3Activities != null) {
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        if (schedule3Activities == null) {
            if (regulation35 == null) {
                assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                }
            }
        } else if (!schedule3Activities.booleanValue()) {
            if (regulation35 == null) {
                assertEquals(
                        "You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and made no selections for the Schedule 3 Activities question. Please review your previous selection.",
                        error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
                }
            }
        } else {
            if (regulation35 == null) {
                assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question. Please review your previous selection.", error);
            } else {
                if (!regulation35.booleanValue()) {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have submitted an unexpected answer for the Regulation 29 question and made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    assertEquals("You have awarded 15 points or more and specified that Regulation 35 applies, but have submitted an unexpected answer for the Regulation 29 question and made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
                }
            }
        }
    }

    @Test
    @Parameters(named = "schedule3ActivityAndRegulation35Combinations")
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreLowAndRequireItToBePopulated_thenDisplayAnError(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        if (schedule3Activities != null) {
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1w");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");

        assertEquals("You have awarded less than 15 points, but have a missing answer for the Regulation 29 question. Please review your previous selection.", error);
    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenRegulation35FieldIsPopulatedWithYesAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        if (schedule3Activities != null) {
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");

        if (schedule3Activities == null) {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", error);

        } else if (!schedule3Activities.booleanValue()) {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", error);
        } else {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", error);
        }

    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenRegulation35FieldIsPopulatedWithNoAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        if (schedule3Activities != null) {
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        if (schedule3Activities == null) {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", error);
        } else if (!schedule3Activities.booleanValue()) {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", error);
        } else {
            assertEquals("You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", error);
        }
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29AndNoOtherFieldsPopoulated_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToNo_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded less than 15 points, specified that Regulation 29 applies and specified that Regulation 35 does not apply, but have made selections for the Schedule 3 Activities question. Please review your previous selection.", error);

    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToYes_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded less than 15 points, specified that Regulation 29 applies and specified that Regulation 35 applies, but have made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded less than 15 points, specified that Regulation 29 applies "
                + "and not provided an answer to the Regulation 35 question, but have made "
                + "no selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToNo_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToYes_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1,  response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more and specified that Regulation 35 does not apply, but have made selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question "
                + "and made no selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }
    
    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
            Arrays.asList("mobilisingUnaided"));
        sscsCaseData.setWcaAppeal(descriptorFlowValue);

        // < 15 points - correct for these fields
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }
}
