package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIDENTIALITY_CONFIRMED;

import java.util.Collections;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUndetermined;

@ExtendWith(MockitoExtension.class)
class ConfidentialityConfirmedMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ConfidentialityConfirmedMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        handler = new ConfidentialityConfirmedMidEventHandler(true);
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"MID_EVENT"}, mode = EnumSource.Mode.EXCLUDE)
    void givenNonMidEvent_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, mode = EnumSource.Mode.EXCLUDE, names = {"CHILD_SUPPORT", "UC"})
    void givenNonSupportedBenefit_thenReturnFalse(Benefit benefit) {
        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefit.getShortName()));

        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CONFIDENTIALITY_CONFIRMED"})
    void givenNonConfidentialityConfirmedEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventAndSupportedBenefit_thenReturnTrue(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventWithSingleOtherPartyAndConfidentialitySet_thenRunSuccessfully(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());
        sscsCaseData.setOtherParties(Collections.singletonList(buildOtherParty("1", YesNoUndetermined.YES)));

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventWithNoOtherPartyAndConfidentialitySet_thenRunSuccessfully(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequirement(YesNoUndetermined.YES).build());

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventWithNullOtherPartyAndConfidentialitySet_thenRunSuccessfully(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());
        CcdValue<OtherParty> nullParty = null;
        sscsCaseData.setOtherParties(Collections.singletonList(nullParty));

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventWithConfidentialityMissing_thenReturnError(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());

        CcdValue<OtherParty> otherPartyWithConfidentiality = buildOtherParty("1", YesNoUndetermined.NO);
        CcdValue<OtherParty> otherPartyWithoutConfidentiality = buildOtherParty("2", null);
        sscsCaseData.setOtherParties(List.of(otherPartyWithConfidentiality, otherPartyWithoutConfidentiality));

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Confidentiality for all parties is required.");
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventWithAppellantConfidentialityMissing_thenReturnError(Benefit benefit) {
        var sscsCaseData = caseDataWithBenefit(benefit.getShortName());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().build());

        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Confidentiality for all parties is required.");
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);

        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION)).isInstanceOf(
            IllegalStateException.class);
    }

    @Test
    void givenCmOtherPartyConfidentialityFlagIsDisabled_thenReturnFalse() {
        assertThat(new ConfidentialityConfirmedMidEventHandler(false).canHandle(MID_EVENT, callback)).isFalse();
    }

    private CcdValue<OtherParty> buildOtherParty(String id, YesNoUndetermined confidentiality) {
        return CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id(id)
                .confidentialityRequirement(confidentiality)
                .build())
            .build();
    }

    private SscsCaseData caseDataWithBenefit(String benefitCode) {
        return SscsCaseData
            .builder()
            .ccdCaseId("1234")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build())
            .build();
    }
}
