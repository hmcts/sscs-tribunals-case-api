package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT_SEND_TO_INTERLOC;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

class NonCompliantSendToInterlocAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private NonCompliantSendToInterlocAboutToStartHandler handler;
    private Callback<SscsCaseData> callback;

    @BeforeEach
    void setUp() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        CaseDetails<SscsCaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.WITH_DWP, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), NON_COMPLIANT_SEND_TO_INTERLOC, false);
        handler = new NonCompliantSendToInterlocAboutToStartHandler(false);
    }

    @Test
    void canHandleForNonCompliantSendToInterloc() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void doesNotSetSelectionForNonChildSupport() {
        var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty()).isNull();
    }

    @Test
    void clearsDefaultSelectionForChildSupport() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());

        final var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty()).isNull();
    }

    @Test
    void givenFlagEnabled_doesNotSetSelectionForNonChildSupport() {
        handler = new NonCompliantSendToInterlocAboutToStartHandler(true);

        final var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"UC", "CHILD_SUPPORT"})
    void givenFlagEnabled_setsEmptyDefaultSelectionForChildSupport(Benefit benefit) {
        handler = new NonCompliantSendToInterlocAboutToStartHandler(true);
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefit.getShortName()).build());

        final var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        final DynamicListItem selectedValue = response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty().getValue();
        assertThat(selectedValue.getCode()).isEmpty();
    }

    @Test
    void throwsAnIllegalStateExceptionIfItIsNotAnNonCompliantSendToInterlocAboutToStartCallback() {
        Callback<SscsCaseData> wrongCallback =
                new Callback<>(callback.getCaseDetails(), Optional.of(callback.getCaseDetails()), EventType.APPEAL_RECEIVED, false);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, wrongCallback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
    }
}
