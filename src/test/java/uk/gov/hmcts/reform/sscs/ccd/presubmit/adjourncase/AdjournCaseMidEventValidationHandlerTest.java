package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

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

    @Mock
    private UserInfo userInfo;


    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new AdjournCaseMidEventValidationHandler(validator);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserInfo("Bearer token")).thenReturn(userInfo);

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

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("7");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Cannot specify both directions due date and directions due days offset", error);
    }

    @Test
    public void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecifiedButZero_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("0");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenDirectionsDueDateIsNotSpecifiedAndDaysOffsetSpecified_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset("7");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNeitherDirectionsDueDateOrOffsetSpecified_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(YES.getValue());

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
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("'First available date after' date cannot be in the past", error);
    }

    @Test
    public void givenNextHearingDateNotRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingDateNotSet_ThenDisplayAnError() {

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("'First available date after' date must be provided", error);

    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
