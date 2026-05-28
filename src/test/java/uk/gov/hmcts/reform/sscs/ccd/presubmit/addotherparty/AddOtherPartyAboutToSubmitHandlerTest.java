package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.VALIDITY_CHALLENGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AddOtherPartyAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;
    private AddOtherPartyAboutToSubmitHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AddOtherPartyAboutToSubmitHandler(true);
        sscsCaseData = SscsCaseData
            .builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(UC.getShortName()).build()).build())
            .build();
        lenient().when(callback.getEvent()).thenReturn(ADD_OTHER_PARTY_DATA);
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void shouldThrowExceptionIfCallbackTypeIsNull() {
        assertThatThrownBy(() -> handler.canHandle(null, callback))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("callbackType must not be null");
    }

    @Test
    void shouldThrowExceptionIfCallbackIsNull() {
        assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_SUBMIT, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("callback must not be null");
    }

    @Test
    void shouldReturnFalseWhenFeatureIsDisabled() {
        handler = new AddOtherPartyAboutToSubmitHandler(false);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCallbackTypeIsNotAboutToSubmit() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenEventIsNotAddOtherPartyData() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenBenefitTypeIsNotUc() {
        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenCannotHandle() {
        final AddOtherPartyAboutToSubmitHandler disabledHandler = new AddOtherPartyAboutToSubmitHandler(false);

        assertThatThrownBy(() -> disabledHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot handle callback");
    }

    @Test
    void shouldClearDwpDueDateAndHmctsDwpState() {
        sscsCaseData.setDwpDueDate("2026-01-01");
        sscsCaseData.setDwpState(VALIDITY_CHALLENGE);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpDueDate()).isNull();
        assertThat(response.getData().getDwpState()).isNull();
    }

}
