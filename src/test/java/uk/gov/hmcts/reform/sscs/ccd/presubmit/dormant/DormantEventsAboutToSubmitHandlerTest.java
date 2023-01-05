package uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import java.util.Collections;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@RunWith(JUnitParamsRunner.class)
public class DormantEventsAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private DormantEventsAboutToSubmitHandler handler;

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

        handler = new DormantEventsAboutToSubmitHandler(listAssistHearingMessageHelper, false);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getState()).thenReturn(INTERLOCUTORY_REVIEW_STATE);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(INTERLOCUTORY_REVIEW_STATE)
                .interlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"HMCTS_LAPSE_CASE", "CONFIRM_LAPSED", "WITHDRAWN", "LAPSED_REVISED", "DORMANT", "ADMIN_SEND_TO_DORMANT_APPEAL_STATE", "ADMIN_APPEAL_WITHDRAWN", "ISSUE_FINAL_DECISION"})
    public void clearInterlocReviewStateAndDirectionDueDateForDormantEvents_whenSchedulingListingDisabled(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        assertThat(response.getData().getPreviousState(), is(INTERLOCUTORY_REVIEW_STATE));
        verifyNoInteractions(listAssistHearingMessageHelper);
    }

    @Test
    @Parameters({"CONFIRM_LAPSED, LAPSED", "ADMIN_SEND_TO_DORMANT_APPEAL_STATE, OTHER", "LAPSED_REVISED, LAPSED",
        "WITHDRAWN, WITHDRAWN"})
    public void sendCancellationReasonAsOther_withEligibleCases_whenSchedulingListingEnabled(EventType eventType,
        CancellationReason cancellationReason) {
        handler = new DormantEventsAboutToSubmitHandler(listAssistHearingMessageHelper, true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(HEARING)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();

        when(callback.getEvent()).thenReturn(eventType);
        when(caseDetails.getState()).thenReturn(HEARING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(listAssistHearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()),
            eq(cancellationReason));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
