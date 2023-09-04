package uk.gov.hmcts.reform.sscs.ccd.presubmit.remittofirsttier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier.SendToFirstTierAboutToStartHandler;

@ExtendWith(MockitoExtension.class)
public class RemitToFirstTierAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private RemitToFirstTierAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new RemitToFirstTierAboutToStartHandler(true);

        caseData = SscsCaseData.builder()
            .postHearing(PostHearing.builder()
                .remitToFirstTier(RemitToFirstTier.builder()
                    .remittanceDocument(DocumentLink.builder().build())
                    .build())
                .build())
            .ccdCaseId("1234")
            .build();
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(REMIT_TO_FIRST_TIER);
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
        handler = new RemitToFirstTierAboutToStartHandler(false);
        when(callback.getEvent()).thenReturn(REMIT_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenCase_shouldClearPostHearingFieldsAndReturnWithoutError() {
        when(callback.getEvent()).thenReturn(REMIT_TO_FIRST_TIER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(caseData.getPostHearing().getRemitToFirstTier().getRemittanceDocument()).isNull();
    }
}
