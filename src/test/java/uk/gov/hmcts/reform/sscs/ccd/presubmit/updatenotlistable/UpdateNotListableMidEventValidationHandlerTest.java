package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.CallbackValidationService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.notlistable.NotListableMidEventValidationHandler;

@RunWith(JUnitParamsRunner.class)
public class UpdateNotListableMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateNotListableMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    private CallbackValidationService callbackValidationService;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        callbackValidationService = new CallbackValidationService();
        handler = new UpdateNotListableMidEventValidationHandler(callbackValidationService);

        when(callback.getEvent()).thenReturn(EventType.UPDATE_NOT_LISTABLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUpdateNotListableCaseEvent_thenReturnFalse() {
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

        sscsCaseData.setUpdateNotListableDueDate(LocalDate.now().toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError() {

        sscsCaseData.setUpdateNotListableDueDate(LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError() {

        sscsCaseData.setUpdateNotListableDueDate(LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

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
