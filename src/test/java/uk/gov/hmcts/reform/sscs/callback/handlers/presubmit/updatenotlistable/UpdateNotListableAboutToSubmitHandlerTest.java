package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatenotlistable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatenotlistable.UpdateNotListableAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class UpdateNotListableAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateNotListableAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new UpdateNotListableAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_NOT_LISTABLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build()).directionDueDate(LocalDate.now().toString())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUpdateNotListableCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenUpdateNotListableWithDirectionsFulfilledYes_thenClearNotListableReasonsAndSetStateToReadyToList() {
        sscsCaseData.setNotListableProvideReasons("reason1");
        sscsCaseData.setState(NOT_LISTABLE);
        sscsCaseData.setUpdateNotListableDirectionsFulfilled("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getNotListableProvideReasons());
        assertEquals(READY_TO_LIST, response.getData().getState());
        assertNull(response.getData().getDirectionDueDate());
        assertEquals(YesNo.YES, response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableWithDirectionsFulfilledNoAndCertainCondition_thenTriggerReadyToList() {
        sscsCaseData.setState(NOT_LISTABLE);
        sscsCaseData.setUpdateNotListableDirectionsFulfilled("no");
        sscsCaseData.setUpdateNotListableInterlocReview("no");
        sscsCaseData.setUpdateNotListableSetNewDueDate("no");
        sscsCaseData.setUpdateNotListableWhereShouldCaseMoveTo("readyToList");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(READY_TO_LIST, response.getData().getState());
        assertEquals(YesNo.YES, response.getData().getShouldReadyToListBeTriggered());
    }

    @ParameterizedTest
    @CsvSource({
        "no, no, no, withDwp",
        "no, yes, no, readyToList",
        "no, no, yes, readyToList"
    })
    public void givenUpdateNotListableWithDirectionsFulfilledNoAndNoCertainCondition_thenDoNotTriggerReadyToList(String directionsFulfilled, String interlocReview, String setNewDueDate, String whereShouldCaseMoveTo) {
        sscsCaseData.setState(NOT_LISTABLE);
        sscsCaseData.setUpdateNotListableDirectionsFulfilled(directionsFulfilled);
        sscsCaseData.setUpdateNotListableInterlocReview(interlocReview);
        sscsCaseData.setUpdateNotListableSetNewDueDate(setNewDueDate);
        sscsCaseData.setUpdateNotListableWhereShouldCaseMoveTo(whereShouldCaseMoveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableWithDirectionsFulfilledNo_thenDoNotClearNotListableReasonsAndDoNotChangeState() {
        sscsCaseData.setNotListableProvideReasons("reason1");
        sscsCaseData.setState(NOT_LISTABLE);
        sscsCaseData.setUpdateNotListableDirectionsFulfilled("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("reason1", response.getData().getNotListableProvideReasons());
        assertEquals(NOT_LISTABLE, response.getData().getState());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableWithInterlocReviewTcw_thenSetInterlocFields() {
        sscsCaseData.setUpdateNotListableInterlocReview("yes");
        sscsCaseData.setUpdateNotListableWhoReviewsCase("reviewByTcw");
        sscsCaseData.setDirectionDueDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(REVIEW_BY_TCW, response.getData().getInterlocReviewState());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getDirectionDueDate());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableWithInterlocReviewJudge_thenSetInterlocFields() {
        sscsCaseData.setUpdateNotListableInterlocReview("yes");
        sscsCaseData.setUpdateNotListableWhoReviewsCase("reviewByJudge");
        sscsCaseData.setDirectionDueDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(REVIEW_BY_JUDGE, response.getData().getInterlocReviewState());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getDirectionDueDate());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableSetNewDueDateYes_thenWriteToDirectionDueDateField() {
        String tomorrowDate = LocalDate.now().plusDays(1).toString();
        sscsCaseData.setUpdateNotListableSetNewDueDate("Yes");
        sscsCaseData.setUpdateNotListableDueDate(tomorrowDate);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(tomorrowDate, response.getData().getDirectionDueDate());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableSetNewDueDateNo_thenDirectionDueDateFieldIsNull() {
        String tomorrowDate = LocalDate.now().plusDays(1).toString();
        sscsCaseData.setUpdateNotListableSetNewDueDate("No");
        sscsCaseData.setUpdateNotListableDueDate(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDirectionDueDate());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @ParameterizedTest
    @CsvSource({"readyToList, READY_TO_LIST", "withDwp, WITH_DWP"})
    public void givenUpdateNotListableWhatShouldCaseMoveToNextReadyToList_thenSetStateToReadyToList(String nextState, State expectedState) {
        sscsCaseData.setUpdateNotListableWhereShouldCaseMoveTo(nextState);
        sscsCaseData.setNotListableProvideReasons("reason1");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedState, response.getData().getState());
        assertNull(response.getData().getNotListableProvideReasons());
        assertNull(response.getData().getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenUpdateNotListableEvent_thenClearTransientFields_andTriggerReadyToList() {
        sscsCaseData.setUpdateNotListableDueDate(LocalDate.now().toString());
        sscsCaseData.setUpdateNotListableWhereShouldCaseMoveTo("withDwp");
        sscsCaseData.setNotListableProvideReasons("reason1");
        sscsCaseData.setUpdateNotListableSetNewDueDate("No");
        sscsCaseData.setUpdateNotListableWhoReviewsCase("reviewByJudge");
        sscsCaseData.setUpdateNotListableDirectionsFulfilled("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateNotListableDueDate());
        assertNull(response.getData().getUpdateNotListableWhereShouldCaseMoveTo());
        assertNull(response.getData().getNotListableProvideReasons());
        assertNull(response.getData().getUpdateNotListableSetNewDueDate());
        assertNull(response.getData().getUpdateNotListableWhoReviewsCase());
        assertNull(response.getData().getUpdateNotListableDirectionsFulfilled());
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
