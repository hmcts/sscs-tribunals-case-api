package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private CreateBundleAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private BundlingHandler bundlingHandler;

    private SscsCaseData sscsCaseData;

    private PreSubmitCallbackResponse<SscsCaseData> bundlingResponse;

    @Before
    public void setUp() {
        openMocks(this);
        sscsCaseData = SscsCaseData.builder().build();
        bundlingResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        when(bundlingHandler.handle(any())).thenReturn(bundlingResponse);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
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
