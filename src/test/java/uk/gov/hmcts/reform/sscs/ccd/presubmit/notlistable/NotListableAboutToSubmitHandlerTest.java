package uk.gov.hmcts.reform.sscs.ccd.presubmit.notlistable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NOT_LISTABLE;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@RunWith(JUnitParamsRunner.class)
public class NotListableAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private NotListableAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private ListAssistHearingMessageHelper listAssistHearingMessageHelper;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new NotListableAboutToSubmitHandler(listAssistHearingMessageHelper, false);

        when(callback.getEvent()).thenReturn(NOT_LISTABLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getState()).thenReturn(State.APPEAL_CREATED);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonNotListableCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenNotListableDueDate_thenWriteToDirectionDueDateField() {
        String tomorrowDate = LocalDate.now().plus(1, ChronoUnit.DAYS).toString();
        sscsCaseData.setNotListableDueDate(tomorrowDate);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(tomorrowDate, response.getData().getDirectionDueDate());
        assertNull(tomorrowDate, response.getData().getNotListableDueDate());
        verifyNoInteractions(listAssistHearingMessageHelper);
    }

    @Test
    @Parameters({"HEARING", "READY_TO_LIST"})
    public void sendListAssistCancelHearingMessage_whenInvolvedInCorrectState(State state) {

        handler = new NotListableAboutToSubmitHandler(listAssistHearingMessageHelper, true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(state)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();

        when(caseDetails.getState()).thenReturn(state);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        when(callback.getEvent()).thenReturn(NOT_LISTABLE);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(listAssistHearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(sscsCaseData), eq(CancellationReason.OTHER));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
