package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpRequestTimeExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Test
    @Parameters({
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_SUBMIT, true, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_START, false, false",
        "DWP_REQUEST_TIME_EXTENSION, null, false, false",
        "null, ABOUT_TO_SUBMIT, false, true",
    })
    public void canHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType, boolean expected,
                          boolean mockNeeded) {
        if (mockNeeded) {
            when(callback.getEvent()).thenReturn(eventType);
        }

        DwpRequestTimeExtensionAboutToSubmitHandler handler = new DwpRequestTimeExtensionAboutToSubmitHandler();

        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expected, actualResult);
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_canHandleThrowException() {
        DwpRequestTimeExtensionAboutToSubmitHandler handler = new DwpRequestTimeExtensionAboutToSubmitHandler();
        handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, null);
    }

    @Test
    public void handle() {
    }
}