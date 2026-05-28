package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AddOtherPartyAboutToSubmitEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private AddOtherPartyAboutToSubmitEventHandler handler;

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
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            assertThatThrownBy(() -> handler.canHandle(null, callback))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callbackType must not be null");
        }

        @Test
        void shouldThrowExceptionIfCallbackIsNull() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_SUBMIT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
        }

        @Test
        void shouldReturnFalseIfCallbackTypeIsNotAboutToSubmit() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
        }

        @Test
        void shouldReturnFalseIfEventTypeIsNotAddOtherPartyData() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @Test
        void shouldReturnFalseIfCaseDetailsIsNull() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(null);
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @Test
        void shouldReturnFalseIfCaseDataIsNull() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(null);
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("canHandleTestParameters")
        void shouldReturnExpectedResultForCanHandle(final boolean cmEnabled, final EventType eventType, final String benefitCode,
            final boolean expected) {
            handler = new AddOtherPartyAboutToSubmitEventHandler(cmEnabled);
            sscsCaseData.getAppeal().getBenefitType().setCode(benefitCode);
            lenient().when(callback.getEvent()).thenReturn(eventType);
            lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
            lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isEqualTo(expected);
        }

        private static Stream<Arguments> canHandleTestParameters() {
            return Stream.of(Arguments.of(true, ADD_OTHER_PARTY_DATA, UC.getShortName(), true),
                Arguments.of(false, ADD_OTHER_PARTY_DATA, UC.getShortName(), false),
                Arguments.of(true, ADD_OTHER_PARTY_DATA, CHILD_SUPPORT.getShortName(), false),
                Arguments.of(true, APPEAL_RECEIVED, UC.getShortName(), false));
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @BeforeEach
        void setup() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(true);
            sscsCaseData.getAppeal().getBenefitType().setCode(UC.getShortName());
            lenient().when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
            lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
            lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        }

        @Test
        void shouldThrowExceptionIfCannotHandle() {
            handler = new AddOtherPartyAboutToSubmitEventHandler(false);
            assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot handle callback");
        }

        @Test
        void shouldSetInterlocReviewStateToHefIssuedAndDueDateTo21DaysInTheFuture() {
            final var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.HEF_ISSUED);
            assertThat(response.getData().getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(21).toString());
        }

        @Test
        void shouldReturnCaseDataInResponse() {
            final var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData()).isSameAs(sscsCaseData);
        }
    }
}
