package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private AdjournCaseMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        handler = new AdjournCaseMidEventValidationHandler();

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenDirectionsDueDateIsToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("7");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Cannot specify both directions due date and directions due days offset", error);
    }

    @Test
    public void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecifiedButZero_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("0");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenDirectionsDueDateIsNotSpecifiedAndDaysOffsetSpecified_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("7");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNeitherDirectionsDueDateOrOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("yes");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one of directions due date or directions due date offset must be specified", error);
    }

    @Test
    public void givenNeitherDirectionsDueDateOrOffsetSpecifiedButNoDirectionsToParties_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("no");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties("no");

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingSpecificDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingSpecificDateIsToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingSpecificDateBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Specified date cannot be in the past", error);
    }

    @Test
    public void givenNextHearingDateNotRequiredForDateOrPeriodAndNextHearingSpecificDateBeforeToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingSpecificDateNotSet_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Specified date must be provided", error);

    }

    @Test
    public void givenNextHearingDateRequiredForDateOrTimeAndNextHearingSpecificDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrTime("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrtTimeAndNextHearingSpecificDateIsToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrTime("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrTimeAndNextHearingSpecificDateBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrTime("provideDate");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Specified date cannot be in the past", error);
    }

    @Test
    public void givenNextHearingDateNotRequiredForDateOrTimeAndNextHearingSpecificDateBeforeToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrTime("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenNextHearingDateRequiredForDateOrTimeAndNextHearingSpecificDateNotSet_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateOrTime("provideDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Specified date must be provided", error);

    }

    @Test
    public void givenNextHearingDateNotRequiredAndNextHearingSpecificDateBeforeToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingSpecificDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
