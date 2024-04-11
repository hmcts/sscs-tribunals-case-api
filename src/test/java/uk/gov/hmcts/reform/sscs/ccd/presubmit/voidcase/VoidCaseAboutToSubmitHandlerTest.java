package uk.gov.hmcts.reform.sscs.ccd.presubmit.voidcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.PoDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@Slf4j
@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setUp() {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, false);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
            .directionDueDate("tomorrow")
                .appeal(Appeal.builder().build())
                .state(State.HEARING)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();
    }

    @Test
    public void givenANonVoidCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAVoidCaseEventAndSnLFeatureEnabled_thenActionsAndHearingCancel() {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, true);
        when(callback.getEvent()).thenReturn(VOID_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(HEARING);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()),
                eq(CancellationReason.OTHER));
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    public void givenAVoidCaseEventAndSnLFeatureNotEnabled_thenActionsButNoHearingCancel() {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, false);
        when(callback.getEvent()).thenReturn(VOID_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenAdminVoidCaseEventAndSnLFeatureEnabled_thenActionsButNoHearingCancel() {
        handler = new VoidCaseAboutToSubmitHandler(hearingMessageHelper, true);
        when(callback.getEvent()).thenReturn(ADMIN_SEND_TO_VOID_STATE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenVoidCase_thenClearPoFields() {
        sscsCaseData.setPoAttendanceConfirmed(YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build());
        sscsCaseData.setPresentingOfficersHearingLink("link");
        when(callback.getEvent()).thenReturn(ADMIN_SEND_TO_VOID_STATE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
