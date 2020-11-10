package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.validation.Validation;
import javax.validation.Validator;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.MidEventValidationHandler;

@RunWith(JUnitParamsRunner.class)
public class MidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private MidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new MidEventValidationHandler(validator);

        when(callback.getEvent()).thenReturn(EventType.NOT_LISTABLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"NOT_LISTABLE", "UPDATE_NOT_LISTABLE"})
    public void givenAValidMidEventValidationCaseEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenANonValidCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"NOT_LISTABLE", "UPDATE_NOT_LISTABLE"})
    public void givenDirectionsDueDateIsToday_ThenDisplayAnError(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        if (eventType.equals(EventType.NOT_LISTABLE)) {
            sscsCaseData.setNotListableDueDate(LocalDate.now().toString());
        } else {
            sscsCaseData.setUpdateNotListableDueDate(LocalDate.now().toString());
        }

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    @Parameters({"NOT_LISTABLE", "UPDATE_NOT_LISTABLE"})
    public void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        String yesterdayDate = LocalDate.now().plus(-1, ChronoUnit.DAYS).toString();
        if (eventType.equals(EventType.NOT_LISTABLE)) {
            sscsCaseData.setNotListableDueDate(yesterdayDate);
        } else {
            sscsCaseData.setUpdateNotListableDueDate(yesterdayDate);
        }

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    @Parameters({"NOT_LISTABLE", "UPDATE_NOT_LISTABLE"})
    public void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        String tomorrowDate = LocalDate.now().plus(1, ChronoUnit.DAYS).toString();
        if (eventType.equals(EventType.NOT_LISTABLE)) {
            sscsCaseData.setNotListableDueDate(tomorrowDate);
        } else {
            sscsCaseData.setUpdateNotListableDueDate(tomorrowDate);
        }

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
