package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class GetFirstTierDocumentsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GetFirstTierDocumentsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private BundlingHandler bundlingHandler;

    private SscsCaseData sscsCaseData;

    private PreSubmitCallbackResponse<SscsCaseData> bundlingResponse;

    @BeforeEach
    public void setUp() {
        handler = new GetFirstTierDocumentsAboutToSubmitHandler(true, true, bundlingHandler);
        sscsCaseData = SscsCaseData.builder().build();
        bundlingResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenANonGetFirstTierDocumentsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void shouldHandleBundling() {
        when(bundlingHandler.handle(any())).thenReturn(bundlingResponse);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response).isEqualTo(bundlingResponse);
    }
}
