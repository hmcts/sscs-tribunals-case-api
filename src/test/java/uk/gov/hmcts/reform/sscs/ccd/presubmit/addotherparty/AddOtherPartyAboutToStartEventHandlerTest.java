package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@ExtendWith(MockitoExtension.class)
class AddOtherPartyAboutToStartEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private AddOtherPartyAboutToStartEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().build()).build()).build();
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void shouldThrowExceptionIfCallbackTypeIsNull() {
            handler = new AddOtherPartyAboutToStartEventHandler(true);
            assertThatThrownBy(() -> handler.canHandle(null, callback)).isInstanceOf(NullPointerException.class)
                .hasMessage("callbackType must not be null");
        }

        @Test
        void shouldThrowExceptionIfCallbackIsNull() {
            handler = new AddOtherPartyAboutToStartEventHandler(true);
            assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_START, null)).isInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
        }

        @Test
        void shouldReturnFalseIfCallbackTypeIsNotAboutToStart() {
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            handler = new AddOtherPartyAboutToStartEventHandler(true);

            assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
        }

        @Test
        void shouldReturnFalseIfEventTypeIsNotAddOtherPartyData() {
            when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

            handler = new AddOtherPartyAboutToStartEventHandler(true);

            assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("canHandleTestParameters")
        void shouldReturnExpectedResultForCanHandle(boolean cmEnabled, String benefitCode, boolean expected) {
            handler = new AddOtherPartyAboutToStartEventHandler(cmEnabled);
            sscsCaseData.getAppeal().getBenefitType().setCode(benefitCode);
            when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            assertThat(handler.canHandle(ABOUT_TO_START, callback)).isEqualTo(expected);
        }

        private static Stream<Arguments> canHandleTestParameters() {
            return Stream.of(
                Arguments.of(true, CHILD_SUPPORT.getShortName(), true),
                Arguments.of(true, UC.getShortName(), true),
                Arguments.of(false, CHILD_SUPPORT.getShortName(), false),
                Arguments.of(false, UC.getShortName(), false),
                Arguments.of(true, "ESA", false));
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @BeforeEach
        void setup() {
            handler = new AddOtherPartyAboutToStartEventHandler(true);
        }

        @Test
        void shouldThrowExceptionIfCannotHandle() {
            configureCallback(WITH_DWP);
            handler = new AddOtherPartyAboutToStartEventHandler(false);

            assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION)).isInstanceOf(
                IllegalStateException.class).hasMessage("Cannot handle callback");
        }

        @ParameterizedTest
        @MethodSource("successfulHandleTestParameters")
        void shouldHandleSuccessfully(String benefitCode, uk.gov.hmcts.reform.sscs.ccd.domain.State state) {
            sscsCaseData.getAppeal().getBenefitType().setCode(benefitCode);
            configureCallback(state);

            var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(sscsCaseData);
        }

        @ParameterizedTest
        @MethodSource("errorHandleTestParameters")
        void shouldAddErrorWhenStateIsIncorrect(String benefitCode, uk.gov.hmcts.reform.sscs.ccd.domain.State state,
            String expectedError) {
            sscsCaseData.getAppeal().getBenefitType().setCode(benefitCode);
            configureCallback(state);

            var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).hasSize(1);
            assertThat(response.getErrors()).contains(expectedError);
        }

        @Test
        void shouldReturnErrorsWhenFlagsAreTrueButStatesAreWrongForBothTypes() {
            handler = new AddOtherPartyAboutToStartEventHandler(true);
            sscsCaseData.getAppeal().getBenefitType().setCode(CHILD_SUPPORT.getShortName());
            configureCallback(READY_TO_LIST);

            var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).contains(
                "The case must be at state \"Await Other Party Data\" in order to add another party");
            assertThat(response.getErrors()).doesNotContain(
                "The case must be at state \"With FTA\" in order to add another party");
        }

        private static Stream<Arguments> successfulHandleTestParameters() {
            return Stream.of(Arguments.of(CHILD_SUPPORT.getShortName(), AWAIT_OTHER_PARTY_DATA),
                Arguments.of(UC.getShortName(), WITH_DWP));
        }

        private static Stream<Arguments> errorHandleTestParameters() {
            return Stream.of(Arguments.of(CHILD_SUPPORT.getShortName(), READY_TO_LIST,
                    "The case must be at state \"Await Other Party Data\" in order to add another party"),
                Arguments.of(UC.getShortName(), READY_TO_LIST,
                    "The case must be at state \"With FTA\" in order to add another party"));
        }

        private void configureCallback(State state) {
            lenient().when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
            lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
            lenient().when(caseDetails.getState()).thenReturn(state);
        }
    }
}
