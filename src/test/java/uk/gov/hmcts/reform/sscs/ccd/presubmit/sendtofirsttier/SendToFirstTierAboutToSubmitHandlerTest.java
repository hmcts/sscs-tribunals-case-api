package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_FIRST_TIER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPPER_TRIBUNAL_DECISION;

import java.util.List;
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
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
public class SendToFirstTierAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SendToFirstTierAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new SendToFirstTierAboutToSubmitHandler(footerService, true);

        caseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .postHearing(PostHearing.builder()
                    .sendToFirstTier(SendToFirstTier.builder()
                        .decisionDocument(DocumentLink.builder()
                            .documentBinaryUrl("binaryUrl")
                            .documentFilename("filename")
                            .documentUrl("url")
                            .documentHash("hash")
                            .build())
                        .build())
                    .build())
                .build();
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(UPPER_TRIBUNAL_DECISION);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void givenPostHearingsBEnabledFalse_thenReturnFalse() {
        handler = new SendToFirstTierAboutToSubmitHandler(footerService, false);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SendToFirstTierActions.class)
    void shouldReturnWithoutError(SendToFirstTierActions action) {
        List<Hearing> hearings = List.of(Hearing.builder().value(HearingDetails.builder().build()).build());
        caseData.setHearings(hearings);
        caseData.getPostHearing().getSendToFirstTier().setAction(action);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldErrorIfNoSendToFirstTierAction() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).anyMatch(error -> error.contains("decision type"));

    }

    @Test
    void shouldErrorIfNoDecisionDocument() {
        caseData.getPostHearing().getSendToFirstTier().setAction(SendToFirstTierActions.DECISION_REMADE);
        caseData.getPostHearing().getSendToFirstTier().setDecisionDocument(null);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).anyMatch(error -> error.contains("decision document"));
    }

}
