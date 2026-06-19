package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappealcreated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUnknown;

@ExtendWith(MockitoExtension.class)
class ValidAppealCreatedAboutToSubmitTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String CONFIDENTIALITY_ERROR = "Confidentiality status Unknown is only applicable to Child Support and Universal Credit appeal types";

    private final ValidAppealCreatedAboutToSubmit handler = new ValidAppealCreatedAboutToSubmit();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Test
    void givenAValidAppealCreatedAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(VALID_APPEAL_CREATED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonValidAppealCreatedEvent_thenReturnFalse(final EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToSubmitCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenANonCanHandleCallback_thenThrowException() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot handle callback");
    }

    @Test
    void givenAPipAppealWithUnknownConfidentiality_thenAddError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handle(Benefit.PIP, YesNoUnknown.UNKNOWN);

        assertThat(response.getErrors()).containsExactly(CONFIDENTIALITY_ERROR);
    }

    @Test
    void givenAPipAppealWithKnownConfidentiality_thenDontAddError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handle(Benefit.PIP, YesNoUnknown.YES);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenAChildSupportAppealWithUnknownConfidentiality_thenDontAddError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handle(Benefit.CHILD_SUPPORT, YesNoUnknown.UNKNOWN);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenAUcAppealWithUnknownConfidentiality_thenDontAddError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handle(Benefit.UC, YesNoUnknown.UNKNOWN);

        assertThat(response.getErrors()).isEmpty();
    }

    private PreSubmitCallbackResponse<SscsCaseData> handle(final Benefit benefit, final YesNoUnknown confidentialityRequiredAnswer) {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefit.getShortName()).build())
                .appellant(Appellant.builder()
                    .confidentialityRequirement(new DynamicList(
                        new DynamicListItem(confidentialityRequiredAnswer.name(), confidentialityRequiredAnswer.toString()), null))
                    .build())
                .build())
            .build();

        when(callback.getEvent()).thenReturn(VALID_APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
