package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_FIRST_TIER;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class SendToFirstTierAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SendToFirstTierAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new SendToFirstTierAboutToStartHandler(true);

        caseData = SscsCaseData.builder()
            .postHearing(PostHearing.builder()
                .sendToFirstTier(SendToFirstTier.builder()
                    .action(SendToFirstTierActions.DECISION_REMADE)
                    .decisionDocument(DocumentLink.builder().build())
                    .build())
                .build())
            .ccdCaseId("1234")
            .build();
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REQUEST);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsBEnabledFalse_thenReturnFalse() {
        handler = new SendToFirstTierAboutToStartHandler(false);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenCase_shouldClearPostHearingFieldsAndReturnWithoutError() {
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(caseData.getPostHearing().getSendToFirstTier().getAction()).isNull();
        assertThat(caseData.getPostHearing().getSendToFirstTier().getDecisionDocument()).isNull();
    }
}
