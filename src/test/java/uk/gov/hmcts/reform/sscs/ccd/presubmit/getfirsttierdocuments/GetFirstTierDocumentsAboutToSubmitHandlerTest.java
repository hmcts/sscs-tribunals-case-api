package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.bundling.BundleCallback;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

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

    private final ArgumentCaptor<BundleCallback> capture = ArgumentCaptor.forClass(BundleCallback.class);


    @BeforeEach
    public void setUp() {
        handler = new GetFirstTierDocumentsAboutToSubmitHandler(true, true, bundlingHandler);
        sscsCaseData = SscsCaseData.builder().build();
        bundlingResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void shouldHandleBundling() {
        when(bundlingHandler.handle(any())).thenReturn(bundlingResponse);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundlingHandler).handle(capture.capture());
        assertEquals(callback, capture.getValue());
        assertEquals(bundlingResponse, response);
    }
}
