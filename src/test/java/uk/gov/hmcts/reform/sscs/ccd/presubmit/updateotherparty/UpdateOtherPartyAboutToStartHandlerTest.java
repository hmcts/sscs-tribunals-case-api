package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class UpdateOtherPartyAboutToStartHandlerTest {

    private UpdateOtherPartyAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new UpdateOtherPartyAboutToStartHandler(false);
    }

    @Nested
    class CanHandle {

        @Test
        void shouldReturnTrueGivenValidCallback() {
            when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
            when(sscsCaseData.isBenefitType(Benefit.UC)).thenReturn(true);

            boolean result = handler.canHandle(ABOUT_TO_START, callback);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseGivenConfidentialityEnabled() {
            handler = new UpdateOtherPartyAboutToStartHandler(true);

            boolean result = handler.canHandle(ABOUT_TO_START, callback);

            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"ABOUT_TO_START"})
        void shouldReturnFalseGivenInvalidCallbackType(CallbackType callbackType) {
            boolean result = handler.canHandle(callbackType, callback);

            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UPDATE_OTHER_PARTY_DATA"})
        void shouldReturnFalseGivenInvalidEventType(EventType eventType) {
            when(callback.getEvent()).thenReturn(eventType);

            boolean result = handler.canHandle(ABOUT_TO_START, callback);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseGivenNonUcBenefitType() {
            when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
            when(sscsCaseData.isBenefitType(Benefit.UC)).thenReturn(false);

            boolean result = handler.canHandle(ABOUT_TO_START, callback);

            assertThat(result).isFalse();
        }

        @Test
        void shouldThrowNullPointerExceptionGivenNullCallback() {
            assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_START, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
        }

        @Test
        void shouldThrowNullPointerExceptionGivenNullCallbackType() {
            assertThatThrownBy(() -> handler.canHandle(null, callback))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callbacktype must not be null");
        }
    }

    @Nested
    class Handle {

        @Test
        void shouldReturnErrorResponseGivenValidUcCase() {
            when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
            when(sscsCaseData.isBenefitType(Benefit.UC)).thenReturn(true);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, "user_auth");

            assertThat(response.getErrors()).containsOnly("This event is not available for Universal Credit cases");
            assertThat(response.getData()).isEqualTo(sscsCaseData);
        }

        @Test
        void shouldThrowIllegalStateExceptionGivenInvalidScenario() {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

            assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, "user_auth"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot handle callback");
        }
    }
}