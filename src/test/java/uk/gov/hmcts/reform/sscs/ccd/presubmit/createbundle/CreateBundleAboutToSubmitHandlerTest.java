package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.bundling.BundleCallback;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

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

    private final ArgumentCaptor<BundleCallback> capture = ArgumentCaptor.forClass(BundleCallback.class);


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
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void shouldHandleBundling() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundlingHandler).handle(capture.capture());
        assertEquals(callback, capture.getValue());
        assertEquals(bundlingResponse, response);
    }
}
