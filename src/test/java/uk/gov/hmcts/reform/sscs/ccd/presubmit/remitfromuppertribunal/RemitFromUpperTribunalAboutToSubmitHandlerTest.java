package uk.gov.hmcts.reform.sscs.ccd.presubmit.remitfromuppertribunal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
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
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
public class RemitFromUpperTribunalAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private RemitFromUpperTribunalAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new RemitFromUpperTribunalAboutToSubmitHandler(footerService, true);

        caseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .postHearing(PostHearing.builder()
                    .remitFromUpperTribunal(RemitFromUpperTribunal.builder()
                        .remittanceDocument(DocumentLink.builder()
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
        when(callback.getEvent()).thenReturn(REMIT_FROM_UPPER_TRIBUNAL);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(UPPER_TRIBUNAL_DECISION);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void givenPostHearingsBEnabledFalse_thenReturnFalse() {
        handler = new RemitFromUpperTribunalAboutToSubmitHandler(footerService, false);
        when(callback.getEvent()).thenReturn(SEND_TO_FIRST_TIER);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(REMIT_FROM_UPPER_TRIBUNAL);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldErrorIfNoRemittanceDocument() {
        caseData.getPostHearing().getRemitFromUpperTribunal().setRemittanceDocument(null);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(REMIT_FROM_UPPER_TRIBUNAL);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).anyMatch(error -> error.contains("remittance document"));
    }

}
