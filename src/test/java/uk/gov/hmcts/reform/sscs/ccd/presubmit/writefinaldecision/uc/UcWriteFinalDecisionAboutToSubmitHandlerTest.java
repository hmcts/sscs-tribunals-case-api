package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeQuestionService;

public class UcWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase<UcDecisionNoticeQuestionService> {

    public UcWriteFinalDecisionAboutToSubmitHandlerTest() throws IOException {
        super(new UcDecisionNoticeQuestionService());
    }


    @SuppressWarnings("unused")
    private static Object[] schedule7ActivityAndSchedule9Paragraph4Combinations() {
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


    @SuppressWarnings("unused")
    private static Object[] schedule7ActivityCombinations() {
        return new Object[]{
            new Boolean[]{null},
            new Boolean[]{false},
            new Boolean[]{true},
        };
    }

    @ParameterizedTest
    @MethodSource("schedule7ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule7ActivitiesNotPopulated(Boolean schedule7Activities, Boolean schedule9Paragraph4) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);

        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule7Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }
        if (schedule9Paragraph4 != null) {
            sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(schedule9Paragraph4.booleanValue() ? YES : NO);
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule7Activities != null && schedule7Activities.booleanValue())
            || schedule7Activities != null && !schedule7Activities.booleanValue() && schedule9Paragraph4 != null) {
            Assertions.assertEquals(0, response.getErrors().size());

        } else {
            Assertions.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule7Activities == null) {
                if (schedule9Paragraph4 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule7Activities.booleanValue()) {
                if (schedule9Paragraph4 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (schedule9Paragraph4 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }

    @ParameterizedTest
    @MethodSource("schedule7ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule7ActivitiesNotPopulated(Boolean schedule7Activities, Boolean schedule9Paragraph4) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);

        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule7Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }
        if (schedule9Paragraph4 != null) {
            sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(schedule9Paragraph4.booleanValue() ? YES : NO);
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule7Activities != null && schedule7Activities.booleanValue())
            || schedule7Activities != null && !schedule7Activities.booleanValue() && schedule9Paragraph4 != null) {
            Assertions.assertEquals(0, response.getErrors().size());

        } else {
            Assertions.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule7Activities == null) {
                if (schedule9Paragraph4 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule7Activities.booleanValue()) {
                if (schedule9Paragraph4 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (schedule9Paragraph4 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Paragraph4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }


    @ParameterizedTest
    @MethodSource("schedule7ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreLowAndRequireItToBePopulated_thenDisplayAnError(Boolean schedule7Activities, Boolean schedule9Paragraph4) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (schedule9Paragraph4 != null) {
            sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(schedule9Paragraph4.booleanValue() ? YES : NO);
        }
        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1w");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");

        assertEquals("You have awarded less than 15 points and specified that Support Group Only Appeal does not apply, but have a missing answer for the Schedule 8 Paragraph 4 question. Please review your previous selection.", error);
    }

    @ParameterizedTest
    @MethodSource("schedule7ActivityCombinations")
    public void givenSchedule9Paragraph4FieldIsPopulatedWithYesAndSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4ButIncorrectForSchedule9Paragraph4_thenDoNoDisplayAnError(Boolean schedule7Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(YES);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());

    }

    @ParameterizedTest
    @MethodSource("schedule7ActivityCombinations")
    public void givenSchedule9Paragraph4FieldIsPopulatedWithNoAndSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4ButIncorrectForSchedule9Paragraph4_thenDisplayAnError(Boolean schedule7Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule7Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply() != null) {
            Assertions.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule7Activities == null) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.",
                    error);
            } else if (!schedule7Activities.booleanValue()) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 9 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.",
                    error);
            } else {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.",
                    error);
            }
        } else {
            Assertions.assertEquals(0, response.getErrors().size());

        }
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertNull(sscsCaseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
        Assertions.assertNotNull(sscsCaseData.getSscsUcCaseData().getSchedule7Selections());
        Assertions.assertFalse(sscsCaseData.getSscsUcCaseData().getSchedule7Selections().isEmpty());

        Assertions.assertEquals(0, response.getErrors().size());

    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertNull(sscsCaseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
        Assertions.assertNotNull(sscsCaseData.getSscsUcCaseData().getSchedule7Selections());
        Assertions.assertFalse(sscsCaseData.getSscsUcCaseData().getSchedule7Selections().isEmpty());

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded less than 15 points, specified that Schedule 8 Paragraph 4 applies and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnErrorButResetSchedule9Paragraph4OnSubmit() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertNull(sscsCaseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
        Assertions.assertNotNull(sscsCaseData.getSscsUcCaseData().getSchedule7Selections());
        Assertions.assertFalse(sscsCaseData.getSscsUcCaseData().getSchedule7Selections().isEmpty());

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertNull(sscsCaseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
        Assertions.assertNotNull(sscsCaseData.getSscsUcCaseData().getSchedule7Selections());
        Assertions.assertFalse(sscsCaseData.getSscsUcCaseData().getSchedule7Selections().isEmpty());

        Assertions.assertEquals(0,  response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }




    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    @Override
    protected DecisionNoticeOutcomeService createOutcomeService(UcDecisionNoticeQuestionService decisionNoticeQuestionService) {
        return new UcDecisionNoticeOutcomeService(decisionNoticeQuestionService);
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(
            Arrays.asList("mobilisingUnaided"));
        sscsCaseData.setWcaAppeal("no".equalsIgnoreCase(descriptorFlowValue) ? NO : YES);
        if ("Yes".equalsIgnoreCase(descriptorFlowValue)) {
            sscsCaseData.setSupportGroupOnlyAppeal("No");
        }

        // < 15 points - correct for these fields
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    @ParameterizedTest
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        // Why do we not need to set valid scenario ?

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals(("Draft Decision Notice generated on %s.pdf".formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    // Refused scenario 1
    @ParameterizedTest
    public void givenNonSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.setSupportGroupOnlyAppeal("No");

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 1 with error due to explicitly allowed
    @ParameterizedTest
    public void givenNonSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenIncorrectlyAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, but have answered No for the Schedule 8 Paragraph 4 question. Please review your previous selection.", error);
    }

    // Refused scenario 1 with error due to support group being set
    @ParameterizedTest
    public void givenNonSupportGroupScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenIncorrectlySupportGroup_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that Support Group Only Appeal applies and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    // Refused scenario 2
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 2 - with error due to explicitly allowed.
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 7 Activities question, but have answered No for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Refused scenario 2 - with error due to support group not being set.
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }


    // Refused Scenario 3
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    // Refused Scenario 3 - with error due to explictly allowed
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 7 Activities question, but have answered No for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Refused Scenario 3 - with error due to non support group answer
    @ParameterizedTest
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Allowed scenario 1
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule7ActivitesAndSchedule9Paragraph4False_WhenAllowed_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    // Allowed scenario 1 - with error due to incorrect setting of refused
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule7ActivitesAndSchedule9Paragraph4False_WhenRefused_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(NO);

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);
    }

    // Allowed scenario 1 - with error due to missing schedule 9 paragraph 4
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule7ActivitesAndSchedule9Paragraph4NotSpecified_WhenAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    // Allowed scenario 2
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndSchedule7ActivitesAndSchedule9Paragraph4NotSet_WhenAllowed_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided"));

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(0, response.getErrors().size());
    }

    // Allowed scenario 2 - with error due to incorrectly refused
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndSchedule7ActivitesAndSchedule9Paragraph4NotSet_WhenRefused_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided"));

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    // Allowed scenario 2 - with error due to no schedule 3 answers
    @ParameterizedTest
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule7ActivitesAndSchedule9Paragraph4NotSet_WhenAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList(""));

        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assertions.assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }
}
