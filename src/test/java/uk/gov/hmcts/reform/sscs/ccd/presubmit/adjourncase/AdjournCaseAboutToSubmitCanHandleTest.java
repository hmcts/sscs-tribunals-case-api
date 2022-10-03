package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

class AdjournCaseAboutToSubmitCanHandleTest extends AdjournCaseAboutToSubmitHandlerTestBase {

    @DisplayName("Given a non about to submit callback type, then return false")
    @ParameterizedTest
    @ValueSource(strings = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenNonAboutToSubmitCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @DisplayName("Throws exception if it cannot handle the appeal")
    @Test
    void givenCannotHandleAppeal_thenThrowsException() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("Given a non adjourn case event, then return false")
    @Test
    void givenNonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @DisplayName("Given caseDetails is null, then return false")
    @Test
    void givenCaseDetailsIsNull_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @DisplayName("Given caseData is null, then return false")
    @Test
    void givenCaseDataIsNull_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

}
