package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class EsaWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase<EsaDecisionNoticeQuestionService> {

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
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule3ActivitiesNotPopulated(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);

        if (schedule3Activities != null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(schedule3Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule3Activities != null && schedule3Activities.booleanValue())
            || schedule3Activities != null && !schedule3Activities.booleanValue() && regulation35 != null) {
            Assert.assertEquals(0, response.getErrors().size());

        } else {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule3Activities == null) {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule3Activities.booleanValue()) {
                if (regulation35 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have made no selections for the Schedule 3 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }

    @Test
    @Parameters(named = "schedule3ActivityAndRegulation35Combinations")
    public void givenRegulation29FieldIsPopulatedWithNoAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule3ActivitiesNotPopulated(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        if (schedule3Activities != null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(schedule3Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule3Activities != null && schedule3Activities.booleanValue())
            || schedule3Activities != null && !schedule3Activities.booleanValue() && regulation35 != null) {
            Assert.assertEquals(0, response.getErrors().size());

        } else {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule3Activities == null) {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule3Activities.booleanValue()) {
                if (regulation35 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have made no selections for the Schedule 3 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Regulation 29 question and have a missing answer for the Schedule 3 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }


    @Test
    @Parameters(named = "schedule3ActivityAndRegulation35Combinations")
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreLowAndRequireItToBePopulated_thenDisplayAnError(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWcaAppeal("Yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        if (schedule3Activities != null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesRegulation35Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1w");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");

        assertEquals("You have awarded less than 15 points, but have a missing answer for the Regulation 29 question. Please review your previous selection.", error);
    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenRegulation35FieldIsPopulatedWithYesAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDoNoDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal(YesNo.YES.getValue());
        if (schedule3Activities != null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());

    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenRegulation35FieldIsPopulatedWithNoAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWcaAppeal(YesNo.YES.getValue());
        if (schedule3Activities != null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(schedule3Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        }

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply() != null) {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule3Activities == null) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.",
                    error);
            } else if (!schedule3Activities.booleanValue()) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Regulation 35 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.",
                    error);
            } else {
                assertEquals(
                    "You have awarded less than 15 points and specified that Regulation 29 does not apply, but have submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.",
                    error);
            }
        } else {
            Assert.assertEquals(0, response.getErrors().size());

        }
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getRegulation35Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule3Selections());
        Assert.assertFalse(sscsCaseData.getSchedule3Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToYes_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getRegulation35Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule3Selections());
        Assert.assertFalse(sscsCaseData.getSchedule3Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

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

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnErrorButResetRegulation35OnSubmit() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getRegulation35Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule3Selections());
        Assert.assertFalse(sscsCaseData.getSchedule3Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndActivitiesSelectedAndRegulation35SetToYes_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getRegulation35Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule3Selections());
        Assert.assertFalse(sscsCaseData.getSchedule3Selections().isEmpty());

        Assert.assertEquals(0,  response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }




    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more and not provided an answer to the Regulation 35 question, but have made no selections for the Schedule 3 Activities question. Please review your previous selection.", error);
    }

    @Override
    protected DecisionNoticeOutcomeService createOutcomeService(EsaDecisionNoticeQuestionService decisionNoticeQuestionService) {
        return new EsaDecisionNoticeOutcomeService(decisionNoticeQuestionService);
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
            Arrays.asList("mobilisingUnaided"));
        sscsCaseData.setWcaAppeal(descriptorFlowValue);

        // < 15 points - correct for these fields
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        // Why do we not need to set valid scenario ?

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    // Refused scenario 1
    @Test
    public void givenNonSupportGroupRefusedScenario_Regulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29AndNoOtherFieldsPopulated_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 1 with error due to explicitly allowed
    @Test
    public void givenNonSupportGroupRefusedScenario_Regulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29AndNoOtherFieldsPopulated_WhenIncorrectlyAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, but have answered No for the Regulation 29 question. Please review your previous selection.", error);
    }

    // Refused scenario 1 with error due to support group being set
    @Test
    public void givenNonSupportGroupScenario_Regulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29AndNoOtherFieldsPopulated_WhenIncorrectlySupportGroup_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have answered No for the Regulation 29 question, have a missing answer for the Schedule 3 Activities question and a missing answer for the Regulation 35 question. Please review your previous selection.", error);
    }

    // Refused scenario 2
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 2 - with error due to explicitly allowed.
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 3 Activities question, but have answered No for the Regulation 35 question. Please review your previous selection.", error);

    }

    // Refused scenario 2 - with error due to support group not being set.
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsPopulatedWithYesAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", error);

    }


    // Refused Scenario 3
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused Scenario 3 - with error due to explictly allowed
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 3 Activities question, but have answered No for the Regulation 35 question. Please review your previous selection.", error);

    }

    // Refused Scenario 3 - with error due to non support group answer
    @Test
    public void givenSupportGroupRefusedScenario_Regulation29FieldIsNotPopulatedAndPointsAreCorrectForRegulation29AndNoActivitiesSelectedAndRegulation35SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - high, which means regulation 29 must not apply.
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", error);

    }
}
