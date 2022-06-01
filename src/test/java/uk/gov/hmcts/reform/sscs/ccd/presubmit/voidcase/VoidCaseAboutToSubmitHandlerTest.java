package uk.gov.hmcts.reform.sscs.ccd.presubmit.voidcase;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
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

@RunWith(JUnitParamsRunner.class)
public class VoidCaseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private VoidCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, false);

        when(callback.getEvent()).thenReturn(EventType.ADMIN_SEND_TO_VOID_STATE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").interlocReviewState("interlocState").directionDueDate("tomorrow")
                .appeal(Appeal.builder().build())
                .state(State.HEARING)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();
        when(caseDetails.getState()).thenReturn(HEARING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonVoidCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"VOID_CASE"})
    public void givenAVoidCaseEventAndSnLFeatureEnabled_thenActionsAndHearingCancel(EventType
        eventType) {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, true);
        when(callback.getEvent()).thenReturn(eventType);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getInterlocReviewState());
        Assert.assertNull(response.getData().getDirectionDueDate());
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()));
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    @Parameters({"VOID_CASE"})
    public void givenAVoidCaseEventAndSnLFeatureNotEnabled_thenActionsButNoHearingCancel(EventType
                                                                                            eventType) {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, false);
        when(callback.getEvent()).thenReturn(eventType);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getInterlocReviewState());
        Assert.assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    @Parameters({"ADMIN_SEND_TO_VOID_STATE"})
    public void givenAdminVoidCaseEventAndSnLFeatureEnabled_thenActionsButNoHearingCancel(EventType
                                                                                                            eventType) {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, true);
        when(callback.getEvent()).thenReturn(eventType);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getInterlocReviewState());
        Assert.assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
