package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIDENTIALITY_CONFIRMED;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class ConfidentialityConfirmedAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ConfidentialityConfirmedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Nested
    class CmOtherPartyConfidentialityEnabled {

        @BeforeEach
        void setUp() {
            handler = new ConfidentialityConfirmedAboutToSubmitHandler(true);
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT"}, mode = EnumSource.Mode.EXCLUDE)
        void givenNonAboutToSubmitEvent_thenReturnFalse(CallbackType callbackType) {
            assertThat(handler.canHandle(callbackType, callback)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = Benefit.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "CHILD_SUPPORT"
        })
        void givenNonChildSupportBenefit_thenReturnFalse(Benefit benefit) {
            when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefit.getShortName()));

            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CONFIDENTIALITY_CONFIRMED"})
        void givenNonConfidentialityConfirmedEvent_thenReturnFalse(EventType eventType) {
            when(callback.getEvent()).thenReturn(eventType);

            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @Test
        void givenConfidentialityConfirmedEventAndChildSupportBenefit_thenReturnTrue() {
            var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

            when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
        }

        @Test
        void givenConfidentialityConfirmedEvent_thenClearInterlocReviewState() {
            var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());
            sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);

            when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getInterlocReviewState()).isNull();
            assertThat(response.getData().getDwpState()).isEqualTo(DwpState.UNREGISTERED);
        }

        @Test
        void throwsExceptionIfItCannotHandleTheAppeal() {
            when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);

            assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class CmOtherPartyConfidentialityDisabled {

        @BeforeEach
        void setUp() {
            handler = new ConfidentialityConfirmedAboutToSubmitHandler(false);
        }

        @Test
        void givenCmOtherPartyConfidentialityFlagIsDisabled_thenReturnFalse() {
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }
    }



    private SscsCaseData caseDataWithBenefit(String benefitCode) {
        return SscsCaseData.builder()
            .ccdCaseId("1234")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(benefitCode)
                    .build())
                .build())
            .build();
    }
}
