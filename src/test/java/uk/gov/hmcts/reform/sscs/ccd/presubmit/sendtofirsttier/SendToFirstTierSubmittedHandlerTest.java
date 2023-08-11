package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@ExtendWith(MockitoExtension.class)
public class SendToFirstTierSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final long CASE_ID = 1234L;

    private SendToFirstTierSubmittedHandler handler;

    @Mock
    private CcdCallbackMapService ccdCallbackMapService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;


    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new SendToFirstTierSubmittedHandler(ccdCallbackMapService, true);

        caseData = SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .postHearing(PostHearing.builder()
                    .sendToFirstTier(SendToFirstTier.builder().build())
                    .build())
                .build();
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(UPPER_TRIBUNAL_DECISION);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }

    @Test
    void givenPostHearingsBEnabledFalse_thenReturnFalse() {
        handler = new SendToFirstTierSubmittedHandler(ccdCallbackMapService, false);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SendToFirstTierActions.class)
    void givenRequestPostHearingTypes_shouldReturnCallCorrectCallback(SendToFirstTierActions value) {
        caseData.getPostHearing().setSendToFirstTier(SendToFirstTier.builder()
                .action(value)
                .build());

        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(ccdCallbackMapService.handleCcdCallbackMap(value, caseData))
                .thenReturn(SscsCaseData.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMap(value, caseData);
    }

}
