package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_HEARING_ENQUIRY_FORM;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class IssueHearingEnquiryFormAboutToSubmitTest {

    private IssueHearingEnquiryFormAboutToSubmit handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new IssueHearingEnquiryFormAboutToSubmit(true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
    }

    @Nested
    class CanHandle {

        @Test
        void shouldReturnTrueGivenAValidCallback() {
            when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);

            boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenCmOtherPartyConfidentialityDisabled() {
            handler = new IssueHearingEnquiryFormAboutToSubmit(false);

            boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);

            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, names = "ABOUT_TO_SUBMIT", mode = EnumSource.Mode.EXCLUDE)
        void shouldReturnFalseGivenInvalidCallbackType(CallbackType callbackType) {
            boolean result = handler.canHandle(callbackType, callback);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseGivenInvalidEventType() {
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

            boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);

            assertThat(result).isFalse();
        }

        @Test
        void shouldThrowNullPointerExceptionGivenNullCallback() {
            assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_SUBMIT, null)).isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
        }

        @Test
        void shouldThrowNullPointerExceptionGivenNullCallbackType() {
            assertThatThrownBy(() -> handler.canHandle(null, callback)).isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("callbacktype must not be null");
        }
    }

    @Nested
    class Handle {

        @Test
        void shouldHandleGivenAValidCallback() {
            when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "userAuthorisation");

            assertThat(response.getData().getDirectionDueDate())
                .isEqualTo(now().plusDays(IssueHearingEnquiryFormAboutToSubmit.getHearingResponseExpectedByDays()).toString());
            assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.HEF_ISSUED);
        }

        @Test
        void shouldThrowIllegalStateExceptionWhenCanHandleIsFalse() {
            assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, "userAuthorisation")).isExactlyInstanceOf(
                IllegalStateException.class).hasMessage("Cannot handle callback.");
        }
    }

    @Test
    void getHearingResponseExpectedByDays_returns21() {
        assertThat(IssueHearingEnquiryFormAboutToSubmit.getHearingResponseExpectedByDays()).isEqualTo(21);
    }
}
